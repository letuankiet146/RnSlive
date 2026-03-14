package org.tk.rnslive.services;

import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
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
    private static final ExchangeName DEFAULT_EXCHANGE = ExchangeName.BINANCE;
    
    public OrderBookManager(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
        startCleanupTask();
    }
    
    /**
     * Get orderbook stream for a symbol (lazy initialization)
     */
    public Flux<OrderBook> getOrderBookStream(String symbol) {
        return getOrderBookStream(DEFAULT_EXCHANGE, symbol);
    }

    public Flux<OrderBook> getOrderBookStream(ExchangeName exchangeName, String symbol) {
        return getOrCreateOrderBook(exchangeName, symbol).getOrderBookStream();
    }
    
    /**
     * Get or create orderbook instance for symbol (thread-safe)
     */
    private SingleOrderBookService getOrCreateOrderBook(ExchangeName exchangeName, String symbol) {
        String key = exchangeName.name() + ":" + symbol;
        return orderBooks.computeIfAbsent(key, s -> {
            if (orderBooks.size() >= MAX_SYMBOLS) {
                LOGGER.warn("Max symbols limit reached ({}). Consider cleanup.", MAX_SYMBOLS);
                cleanupInactiveOrderBooks();
            }
            
            LOGGER.info("Creating new orderbook instance for {}: {}", exchangeName, symbol);
            SingleOrderBookService orderBook = new SingleOrderBookService(exchangeService, exchangeName, symbol);
            orderBook.start();
            return orderBook;
        });
    }
    
    /**
     * Cleanup inactive orderbooks to free memory
     */
    public void cleanupInactiveOrderBooks() {
        long now = System.currentTimeMillis();
        orderBooks.entrySet().removeIf(entry -> {
            if (entry.getValue().isForceRemove() || ( now - entry.getValue().getLastAccessTime() > INACTIVE_TIMEOUT_MS)) {
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
