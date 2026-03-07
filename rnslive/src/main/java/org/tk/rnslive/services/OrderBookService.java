package org.tk.rnslive.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.ltk.model.exchange.depth.Depth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tk.rnslive.model.DepthUpdate;
import org.tk.rnslive.model.OrderBook;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class OrderBookService {

    @Autowired
    private ExchangeService exchangeService;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Sinks.Many<OrderBook> orderBookSink = Sinks.many().replay().latest();
    
    private final ConcurrentLinkedQueue<DepthUpdate> updateBuffer = new ConcurrentLinkedQueue<>();
    
    private String symbol;
    private long lastUpdateId;
    private TreeMap<Double, Double> bids;
    private TreeMap<Double, Double> asks;
    private long timestamp;
    
    private boolean isInitialized = false;
    private long lastProcessedUpdateId = -1;
    private boolean firstUpdateProcessed = false;

    public Flux<OrderBook> getOrderBookStream() {
        return orderBookSink.asFlux();
    }

    public Mono<OrderBook> snapshotOrderBook() {
        return getOrderBookStream().next();
    }

    public Mono<Depth> getSnapshotDepth() {
        return exchangeService.getDepth(ExchangeName.BINANCE, symbol, 1000);
    }

    @PostConstruct
    public void init() {
        startOrderBook("BTCUSDT");
    }

    private void startOrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new TreeMap<>((a, b) -> Double.compare(b, a));
        this.asks = new TreeMap<>();
        this.timestamp = System.currentTimeMillis();
        
        LOGGER.info("Step 1: Opening WebSocket stream for {}", symbol);
        exchangeService.subscribeDepth(ExchangeName.BINANCE, symbol, null, this::bufferDepthUpdate);
        
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                initializeOrderBook(symbol);
            } catch (Exception e) {
                LOGGER.error("Error initializing orderbook", e);
            }
        }).start();
    }

    private void bufferDepthUpdate(String json) {
        try {
            DepthUpdate update = parseDepthUpdate(json);
            if (update != null) {
                if (!isInitialized) {
                    updateBuffer.offer(update);
                } else {
                    synchronized (this) {
                        boolean processed = processUpdate(update);
                        LOGGER.info("Real-time update: U={}, u={}, pu={}, processed={}", 
                            update.getFirstUpdateId(), update.getFinalUpdateId(), 
                            update.getPreviousFinalUpdateId(), processed);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error buffering depth update", e);
        }
    }

    private void initializeOrderBook(String symbol) {
        LOGGER.info("Step 3: Fetching depth snapshot for {}", symbol);
        exchangeService.getDepth(ExchangeName.BINANCE, symbol, 1000)
            .subscribe(
                depth -> {
                    try {
                        synchronized (this) {
                            this.lastUpdateId = depth.getLastUpdateId();
                            
                            for (List<Double> bid : depth.getBids()) {
                                updateBid(bid.get(0), bid.get(1));
                            }
                            
                            for (List<Double> ask : depth.getAsks()) {
                                updateAsk(ask.get(0), ask.get(1));
                            }
                            
                            lastProcessedUpdateId = depth.getLastUpdateId();
                            
                            LOGGER.info("Snapshot received with lastUpdateId: {}, buffer size: {}", 
                                depth.getLastUpdateId(), updateBuffer.size());
                            
                            isInitialized = true;
                            
                            processBufferedUpdates();
                            emitOrderBook();
                            
                            LOGGER.info("OrderBook fully initialized for {}", symbol);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing depth snapshot", e);
                    }
                },
                error -> LOGGER.error("Error fetching depth snapshot", error)
            );
    }

    private void processBufferedUpdates() {
        DepthUpdate update;
        int processed = 0;
        int skipped = 0;
        
        while ((update = updateBuffer.poll()) != null) {
            if (processUpdate(update)) {
                processed++;
            } else {
                skipped++;
            }
        }
        
        LOGGER.info("Processed {} buffered updates, skipped {}", processed, skipped);
    }

    private boolean processUpdate(DepthUpdate update) {
        // Step 4: Drop events where u <= lastUpdateId (already processed)
        if (update.getFinalUpdateId() <= lastUpdateId) {
            return false;
        }
        
        // Step 5: First processed event validation
        if (!firstUpdateProcessed) {
            // First event must have U <= lastUpdateId AND u >= lastUpdateId
            if (update.getFirstUpdateId() <= lastUpdateId  &&
                update.getFinalUpdateId() >= lastUpdateId) {
                firstUpdateProcessed = true;
                applyUpdate(update);
                return true;
            } else {
                LOGGER.warn("Skipping first update: U={}, u={}, lastUpdateId={}", 
                    update.getFirstUpdateId(), update.getFinalUpdateId(), lastUpdateId);
                return false;
            }
        }
        
        // Step 6: Verify sequence (pu should equal previous u)
        if (update.getPreviousFinalUpdateId() != lastProcessedUpdateId) {
            LOGGER.error("Sequence break! Expected pu={}, got pu={}. Reinitializing...", 
                lastProcessedUpdateId, update.getPreviousFinalUpdateId());
            reinitialize();
            return false;
        }
        
        applyUpdate(update);
        return true;
    }

    private void applyUpdate(DepthUpdate update) {
        if (update.getBids() != null) {
            for (List<String> bid : update.getBids()) {
                double price = Double.parseDouble(bid.get(0));
                double quantity = Double.parseDouble(bid.get(1));
                updateBid(price, quantity);
            }
        }
        
        if (update.getAsks() != null) {
            for (List<String> ask : update.getAsks()) {
                double price = Double.parseDouble(ask.get(0));
                double quantity = Double.parseDouble(ask.get(1));
                updateAsk(price, quantity);
            }
        }
        
        this.lastUpdateId = update.getFinalUpdateId();
        this.timestamp = update.getEventTime();
        lastProcessedUpdateId = update.getFinalUpdateId();
        
        emitOrderBook();
    }

    private void updateBid(double price, double quantity) {
        if (quantity == 0) {
            bids.remove(price);
        } else {
            bids.put(price, quantity);
        }
    }

    private void updateAsk(double price, double quantity) {
        if (quantity == 0) {
            asks.remove(price);
        } else {
            asks.put(price, quantity);
        }
    }

    private void emitOrderBook() {
        Map<Double, Double> bidsCopy = new TreeMap<>((a, b) -> Double.compare(b, a));
        bidsCopy.putAll(bids);

        Map<Double, Double> asksCopy = new TreeMap<>(asks);

        OrderBook snapshot = new OrderBook(symbol, lastUpdateId, bidsCopy, asksCopy, timestamp);

        Sinks.EmitResult result = orderBookSink.tryEmitNext(snapshot);
        if (result.isFailure()) {
            LOGGER.warn("Failed to emit orderbook: {}", result);
        }
    }

    private void reinitialize() {
        LOGGER.warn("Reinitializing orderbook due to sequence break");
        isInitialized = false;
        firstUpdateProcessed = false;
        updateBuffer.clear();
        lastProcessedUpdateId = -1;
        bids.clear();
        asks.clear();
        initializeOrderBook(symbol);
    }

    private DepthUpdate parseDepthUpdate(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            
            DepthUpdate update = new DepthUpdate();
            update.setE(root.get("e").asText());
            update.setEventTime(root.get("E").asLong());
            update.setTransactionTime(root.get("T").asLong());
            update.setSymbol(root.get("s").asText());
            update.setFirstUpdateId(root.get("U").asLong());
            update.setFinalUpdateId(root.get("u").asLong());
            update.setPreviousFinalUpdateId(root.get("pu").asLong());
            
            JsonNode bidsNode = root.get("b");
            if (bidsNode != null && bidsNode.isArray()) {
                List<List<String>> bids = new java.util.ArrayList<>();
                for (JsonNode bidNode : bidsNode) {
                    List<String> bid = new java.util.ArrayList<>();
                    bid.add(bidNode.get(0).asText());
                    bid.add(bidNode.get(1).asText());
                    bids.add(bid);
                }
                update.setBids(bids);
            }
            
            JsonNode asksNode = root.get("a");
            if (asksNode != null && asksNode.isArray()) {
                List<List<String>> asks = new java.util.ArrayList<>();
                for (JsonNode askNode : asksNode) {
                    List<String> ask = new java.util.ArrayList<>();
                    ask.add(askNode.get(0).asText());
                    ask.add(askNode.get(1).asText());
                    asks.add(ask);
                }
                update.setAsks(asks);
            }
            
            return update;
        } catch (Exception e) {
            LOGGER.error("Error parsing depth update", e);
            return null;
        }
    }
}
