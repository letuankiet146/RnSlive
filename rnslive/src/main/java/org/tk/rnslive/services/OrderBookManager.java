package org.tk.rnslive.services;

import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.ltk.model.exchange.depth.Depth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tk.rnslive.model.OrderBook;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple orderbook instances for different symbols
 * Performance optimizations:
 * - Lazy initialization (only create orderbook when requested)
 * - ConcurrentHashMap for thread-safe symbol lookup
 * - Automatic cleanup of inactive orderbooks
 */
@Service
public class OrderBookManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookManager.class);
    
    private final ExchangeService exchangeService;
    private final Map<String, SingleOrderBookService> orderBooks = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_SYMBOLS = 100; // Prevent memory exhaustion
    private static final long INACTIVE_TIMEOUT_MS = 300000; // 5 minutes
    
    public OrderBookManager(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
        startCleanupTask();
    }
    
    /**
     * Get orderbook stream for a symbol (lazy initialization)
     */
    public Flux<OrderBook> getOrderBookStream(String symbol) {
        return getOrCreateOrderBook(symbol).getOrderBookStream();
    }
    
    /**
     * Get orderbook snapshot for a symbol
     */
    public Mono<OrderBook> snapshotOrderBook(String symbol) {
        return getOrCreateOrderBook(symbol).snapshotOrderBook();
    }
    
    /**
     * Get depth snapshot for a symbol
     */
    public Mono<Depth> getSnapshotDepth(String symbol) {
        return exchangeService.getDepth(ExchangeName.BINANCE, symbol, 1000);
    }
    
    /**
     * Get or create orderbook instance for symbol (thread-safe)
     */
    private SingleOrderBookService getOrCreateOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, s -> {
            if (orderBooks.size() >= MAX_SYMBOLS) {
                LOGGER.warn("Max symbols limit reached ({}). Consider cleanup.", MAX_SYMBOLS);
                cleanupInactiveOrderBooks();
            }
            
            LOGGER.info("Creating new orderbook instance for symbol: {}", s);
            SingleOrderBookService orderBook = new SingleOrderBookService(exchangeService, s);
            orderBook.start();
            return orderBook;
        });
    }
    
    /**
     * Manually stop and remove orderbook for a symbol
     */
    public void stopOrderBook(String symbol) {
        SingleOrderBookService orderBook = orderBooks.remove(symbol);
        if (orderBook != null) {
            orderBook.stop();
            LOGGER.info("Stopped orderbook for symbol: {}", symbol);
        }
    }
    
    /**
     * Cleanup inactive orderbooks to free memory
     */
    private void cleanupInactiveOrderBooks() {
        long now = System.currentTimeMillis();
        orderBooks.entrySet().removeIf(entry -> {
            if (now - entry.getValue().getLastAccessTime() > INACTIVE_TIMEOUT_MS) {
                LOGGER.info("Cleaning up inactive orderbook: {}", entry.getKey());
                entry.getValue().stop();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Background task to periodically cleanup inactive orderbooks
     */
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Check every minute
                    cleanupInactiveOrderBooks();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("OrderBook-Cleanup");
        cleanupThread.start();
    }
    
    /**
     * Get statistics about active orderbooks
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "activeSymbols", orderBooks.size(),
            "maxSymbols", MAX_SYMBOLS,
            "symbols", orderBooks.keySet()
        );
    }
}
