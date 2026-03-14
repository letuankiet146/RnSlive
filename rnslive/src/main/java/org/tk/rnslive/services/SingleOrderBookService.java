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

import org.ltk.connector.utils.SecurityHelper;

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
    private final ExchangeName exchangeName;
    
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

    // OKX raw price/size strings for checksum
    private TreeMap<Double, String[]> okxBidsRaw;
    private TreeMap<Double, String[]> okxAsksRaw;
    
    // State flags
    private volatile boolean isInitialized = false;
    private volatile boolean isRunning = false;
    private long lastProcessedUpdateId = -1;
    private boolean firstUpdateProcessed = false;

    // OKX-specific sequencing
    private long okxLastSeqId = -1;

    public SingleOrderBookService(ExchangeService exchangeService, ExchangeName exchangeName, String symbol) {
        this.exchangeService = exchangeService;
        this.exchangeName = exchangeName;
        this.symbol = symbol;
        this.orderBookSink = Sinks.many().replay().latest();
        this.updateBuffer = new ConcurrentLinkedQueue<>();
        this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        this.bids = new TreeMap<>((a, b) -> Double.compare(b, a));
        this.asks = new TreeMap<>();
        if (exchangeName == ExchangeName.OKX) {
            this.okxBidsRaw = new TreeMap<>((a, b) -> Double.compare(b, a));
            this.okxAsksRaw = new TreeMap<>();
        }
    }

    public void start() {
        if (isRunning) {
            LOGGER.warn("OrderBook for {} is already running", symbol);
            return;
        }
        
        isRunning = true;
        this.timestamp = System.currentTimeMillis();
        
        LOGGER.info("Starting WebSocket stream for {}", symbol);
        exchangeService.subscribeDepth(exchangeName, symbol, null, this::bufferDepthUpdate);
        
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
            if (exchangeName == ExchangeName.BINANCE) {
                DepthUpdate update = parseBinanceDepthUpdate(json);
                if (update != null && update.getSymbol().equals(symbol)) {
                    if (!isInitialized) {
                        updateBuffer.offer(update);
                    } else {
                        synchronized (this) {
                            processUpdate(update);
                        }
                    }
                }
            } else if (exchangeName == ExchangeName.OKX) {
                synchronized (this) {
                    handleOkxDepthMessage(json);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error buffering depth update for {}", symbol, e);
        }
    }

    private void initializeOrderBook() {
        // For Binance we bootstrap from REST depth; for OKX we rely solely on
        // the WebSocket snapshot message from the `books` channel.
        if (exchangeName == ExchangeName.BINANCE) {
            LOGGER.info("Fetching depth snapshot for {}", symbol);
            exchangeService.getDepth(exchangeName, symbol, 1000)
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
        } else {
            LOGGER.info("Skipping REST depth initialization for {} on exchange {}", symbol, exchangeName);
        }
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
        if (okxBidsRaw != null) {
            okxBidsRaw.clear();
        }
        if (okxAsksRaw != null) {
            okxAsksRaw.clear();
        }
        okxLastSeqId = -1;

        if (exchangeName == ExchangeName.BINANCE) {
            initializeOrderBook();
        } else {
            // For OKX we rely on the next WebSocket `snapshot` message from the `books` channel
            LOGGER.info("Waiting for next OKX WS snapshot to reinitialize orderbook for {}", symbol);
        }
    }

    private DepthUpdate parseBinanceDepthUpdate(String json) {
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

    private void handleOkxDepthMessage(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray() || dataNode.isEmpty()) {
                return;
            }

            JsonNode book = dataNode.get(0);
            JsonNode bidsNode = book.get("bids");
            JsonNode asksNode = book.get("asks");
            String action = root.has("action") ? root.get("action").asText() : "update";

            long seqId = book.has("seqId") ? book.get("seqId").asLong() : -1;
            long prevSeqId = book.has("prevSeqId") ? book.get("prevSeqId").asLong() : -1;

            // Sequence handling per OKX spec
            if ("snapshot".equals(action)) {
                // First full snapshot from WS initializes the local book
                okxLastSeqId = seqId;
                applyOkxSnapshot(bidsNode, asksNode, book);
                isInitialized = true;
                return;
            }

            // update
            if (!isInitialized) {
                // Ignore updates until we've seen the initial snapshot
                LOGGER.error("OKX depth missing snapshot before incremental updating for {}", symbol);
                return;
            }
            if (okxLastSeqId != -1) {
                // Exception 1: heartbeat (no changes, seqId == prevSeqId == okxLastSeqId)
                if ((bidsNode == null || bidsNode.isEmpty())
                        && (asksNode == null || asksNode.isEmpty())
                        && seqId == okxLastSeqId
                        && prevSeqId == okxLastSeqId) {
                    return;
                }

                // Normal case: prevSeqId should match last seqId
                if (prevSeqId != okxLastSeqId) {
                    LOGGER.error("OKX sequence mismatch for {}: expected prevSeqId={}, got prevSeqId={}, seqId={}. Reinitializing.",
                            symbol, okxLastSeqId, prevSeqId, seqId);
                    reinitialize();
                    return;
                }
            }

            okxLastSeqId = seqId;
            applyOkxDelta(bidsNode, asksNode, book);

        } catch (Exception e) {
            LOGGER.error("Error applying OKX message for {}", symbol, e);
        }
    }

    private void applyOkxSnapshot(JsonNode bidsNode, JsonNode asksNode, JsonNode book) {
        // Reset local book and apply full snapshot
        bids.clear();
        asks.clear();
        applyOkxDelta(bidsNode, asksNode, book);
    }

    private void applyOkxDelta(JsonNode bidsNode, JsonNode asksNode, JsonNode book) {
        // Merge incremental updates into local full book, following OKX rules:
        // - Same price: if size == 0 remove, else replace.
        // - New price: insert at correct level (TreeMap handles ordering).
        if (bidsNode != null && bidsNode.isArray()) {
            for (JsonNode bidNode : bidsNode) {
                if (bidNode.size() < 2) {
                    continue;
                }
                String pxStr = bidNode.get(0).asText();
                String szStr = bidNode.get(1).asText();
                double price = Double.parseDouble(pxStr);
                double quantity = Double.parseDouble(szStr);

                if (quantity == 0d) {
                    bids.remove(price);
                    if (okxBidsRaw != null) {
                        okxBidsRaw.remove(price);
                    }
                } else {
                    bids.put(price, quantity);
                    if (okxBidsRaw != null) {
                        okxBidsRaw.put(price, new String[]{pxStr, szStr});
                    }
                }
            }
        }

        if (asksNode != null && asksNode.isArray()) {
            for (JsonNode askNode : asksNode) {
                if (askNode.size() < 2) {
                    continue;
                }
                String pxStr = askNode.get(0).asText();
                String szStr = askNode.get(1).asText();
                double price = Double.parseDouble(pxStr);
                double quantity = Double.parseDouble(szStr);

                if (quantity == 0d) {
                    asks.remove(price);
                    if (okxAsksRaw != null) {
                        okxAsksRaw.remove(price);
                    }
                } else {
                    asks.put(price, quantity);
                    if (okxAsksRaw != null) {
                        okxAsksRaw.put(price, new String[]{pxStr, szStr});
                    }
                }
            }
        }

        // Checksum validation (OKX CRC32 over first 25 bids & asks)
        if (book.has("checksum") && okxBidsRaw != null && okxAsksRaw != null) {
            int serverChecksum = book.get("checksum").asInt();
            int localChecksum = computeOkxChecksum();
            if (localChecksum != serverChecksum) {
                LOGGER.error("OKX checksum mismatch for {}: local={}, server={}. Reinitializing.", symbol, localChecksum, serverChecksum);
                reinitialize();
                return;
            }
        }

        long ts = book.has("ts") ? book.get("ts").asLong() : System.currentTimeMillis();
        this.lastUpdateId = ts;
        this.timestamp = ts;

        emitOrderBook();
    }

    private int computeOkxChecksum() {
        if (okxBidsRaw.isEmpty() && okxAsksRaw.isEmpty()) {
            return 0;
        }

        // Take first 25 bids (already descending) and first 25 asks (already ascending)
        List<String[]> bidList = new ArrayList<>();
        for (Map.Entry<Double, String[]> e : okxBidsRaw.entrySet()) {
            if (bidList.size() >= 25) break;
            bidList.add(e.getValue());
        }
        List<String[]> askList = new ArrayList<>();
        for (Map.Entry<Double, String[]> e : okxAsksRaw.entrySet()) {
            if (askList.size() >= 25) break;
            askList.add(e.getValue());
        }

        // Interleave: bid → ask → bid → ask ...
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            if (i < bidList.size()) {
                parts.add(bidList.get(i)[0]);
                parts.add(bidList.get(i)[1]);
            }
            if (i < askList.size()) {
                parts.add(askList.get(i)[0]);
                parts.add(askList.get(i)[1]);
            }
        }

        String checkStr = String.join(":", parts);
        return SecurityHelper.calcOkxChecksum(checkStr);
    }
}
