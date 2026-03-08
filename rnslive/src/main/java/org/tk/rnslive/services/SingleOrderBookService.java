package org.tk.rnslive.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tk.rnslive.model.DepthUpdate;
import org.tk.rnslive.model.OrderBook;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single orderbook instance for one symbol
 * Isolated state management for thread safety and performance
 */
public class SingleOrderBookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleOrderBookService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExchangeService exchangeService;
    
    // Symbol-specific state
    private final String symbol;
    private final Sinks.Many<OrderBook> orderBookSink;
    private final ConcurrentLinkedQueue<DepthUpdate> updateBuffer;
    private final AtomicLong lastAccessTime;
    
    // Orderbook data
    private long lastUpdateId;
    private TreeMap<Double, Double> bids;
    private TreeMap<Double, Double> asks;
    private long timestamp;
    
    // State flags
    private volatile boolean isInitialized = false;
    private volatile boolean isRunning = false;
    private long lastProcessedUpdateId = -1;
    private boolean firstUpdateProcessed = false;

    public SingleOrderBookService(ExchangeService exchangeService, String symbol) {
        this.exchangeService = exchangeService;
        this.symbol = symbol;
        this.orderBookSink = Sinks.many().replay().latest();
        this.updateBuffer = new ConcurrentLinkedQueue<>();
        this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        this.bids = new TreeMap<>((a, b) -> Double.compare(b, a));
        this.asks = new TreeMap<>();
    }

    public void start() {
        if (isRunning) {
            LOGGER.warn("OrderBook for {} is already running", symbol);
            return;
        }
        
        isRunning = true;
        this.timestamp = System.currentTimeMillis();
        
        LOGGER.info("Starting WebSocket stream for {}", symbol);
        exchangeService.subscribeDepth(ExchangeName.BINANCE, symbol, null, this::bufferDepthUpdate);
        
        // Initialize after brief delay to allow WebSocket connection
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                initializeOrderBook();
            } catch (Exception e) {
                LOGGER.error("Error initializing orderbook for {}", symbol, e);
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        isInitialized = false;
        updateBuffer.clear();
        orderBookSink.tryEmitComplete();
        LOGGER.info("Stopped orderbook for {}", symbol);
    }

    public Flux<OrderBook> getOrderBookStream() {
        updateLastAccessTime();
        return orderBookSink.asFlux();
    }

    public Mono<OrderBook> snapshotOrderBook() {
        updateLastAccessTime();
        return getOrderBookStream().next();
    }

    public long getLastAccessTime() {
        return lastAccessTime.get();
    }

    private void updateLastAccessTime() {
        lastAccessTime.set(System.currentTimeMillis());
    }

    private void bufferDepthUpdate(String json) {
        if (!isRunning) return;
        
        try {
            DepthUpdate update = parseDepthUpdate(json);
            if (update != null && update.getSymbol().equals(symbol)) {
                if (!isInitialized) {
                    updateBuffer.offer(update);
                } else {
                    synchronized (this) {
                        processUpdate(update);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error buffering depth update for {}", symbol, e);
        }
    }

    private void initializeOrderBook() {
        LOGGER.info("Fetching depth snapshot for {}", symbol);
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
                            LOGGER.info("Snapshot received for {} with lastUpdateId: {}, buffer size: {}",
                                symbol, depth.getLastUpdateId(), updateBuffer.size());

                            isInitialized = true;
                            processBufferedUpdates();
                            emitOrderBook();

                            LOGGER.info("OrderBook fully initialized for {}", symbol);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing depth snapshot for {}", symbol, e);
                    }
                },
                error -> LOGGER.error("Error fetching depth snapshot for {}", symbol, error)
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

        LOGGER.info("Processed {} buffered updates for {}, skipped {}", processed, symbol, skipped);
    }

    private boolean processUpdate(DepthUpdate update) {
        if (update.getFinalUpdateId() <= lastUpdateId) {
            return false;
        }

        if (!firstUpdateProcessed) {
            if (update.getFirstUpdateId() <= lastUpdateId && 
                update.getFinalUpdateId() >= lastUpdateId) {
                firstUpdateProcessed = true;
                applyUpdate(update);
                return true;
            } else {
                LOGGER.warn("Skipping first update for {}: U={}, u={}, lastUpdateId={}",
                    symbol, update.getFirstUpdateId(), update.getFinalUpdateId(), lastUpdateId);
                return false;
            }
        }

        if (update.getPreviousFinalUpdateId() != lastProcessedUpdateId) {
            LOGGER.error("Sequence break for {}! Expected pu={}, got pu={}. Reinitializing...",
                symbol, lastProcessedUpdateId, update.getPreviousFinalUpdateId());
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
        this.timestamp = update.getTransactionTime();
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
            LOGGER.warn("Failed to emit orderbook for {}: {}", symbol, result);
        }
    }

    private void reinitialize() {
        LOGGER.warn("Reinitializing orderbook for {} due to sequence break", symbol);
        isInitialized = false;
        firstUpdateProcessed = false;
        updateBuffer.clear();
        lastProcessedUpdateId = -1;
        bids.clear();
        asks.clear();
        initializeOrderBook();
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
                List<List<String>> bids = new ArrayList<>();
                for (JsonNode bidNode : bidsNode) {
                    List<String> bid = new ArrayList<>();
                    bid.add(bidNode.get(0).asText());
                    bid.add(bidNode.get(1).asText());
                    bids.add(bid);
                }
                update.setBids(bids);
            }

            JsonNode asksNode = root.get("a");
            if (asksNode != null && asksNode.isArray()) {
                List<List<String>> asks = new ArrayList<>();
                for (JsonNode askNode : asksNode) {
                    List<String> ask = new ArrayList<>();
                    ask.add(askNode.get(0).asText());
                    ask.add(askNode.get(1).asText());
                    asks.add(ask);
                }
                update.setAsks(asks);
            }

            return update;
        } catch (Exception e) {
            LOGGER.error("Error parsing depth update for {}", symbol, e);
            return null;
        }
    }
}
