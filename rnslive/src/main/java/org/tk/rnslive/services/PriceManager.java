package org.tk.rnslive.services;

import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.component.Kline;
import org.ltk.connector.service.ExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tk.rnslive.dto.PriceDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple price stream instances for different symbols
 * Performance optimizations:
 * - Lazy initialization (only create price stream when requested)
 * - ConcurrentHashMap for thread-safe symbol lookup
 * - Automatic cleanup of inactive price streams
 */
@Service
public class PriceManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceManager.class);
    
    private final ExchangeService exchangeService;
    private final Map<String, SinglePriceService> priceStreams = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_SYMBOLS = 100; // Prevent memory exhaustion
    private static final long INACTIVE_TIMEOUT_MS = 300000; // 5 minutes
    private static final ExchangeName DEFAULT_EXCHANGE = ExchangeName.BINANCE;
    
    public PriceManager(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
        startCleanupTask();
    }
    
    /**
     * Get price stream for a symbol (lazy initialization)
     */
    public Flux<PriceDto> getPriceStream(String symbol) {
        return getPriceStream(DEFAULT_EXCHANGE, symbol);
    }

    /**
     * Get price stream for a symbol from a specific exchange
     */
    public Flux<PriceDto> getPriceStream(ExchangeName exchangeName, String symbol) {
        return getOrCreatePriceStream(exchangeName, symbol).getPriceStream();
    }
    
    /**
     * Get kLines for a symbol
     */
    public Mono<List<Kline>> getKline(String symbol, String interval) {
        return getKline(DEFAULT_EXCHANGE, symbol, interval);
    }

    public Mono<List<Kline>> getKline(ExchangeName exchangeName, String symbol, String interval) {
        return exchangeService.getKline(exchangeName, symbol, interval, null, null, null);
    }
    
    /**
     * Get or create price stream instance for symbol (thread-safe)
     */
    private SinglePriceService getOrCreatePriceStream(ExchangeName exchangeName, String symbol) {
        String key = exchangeName.name() + ":" + symbol;
        return priceStreams.computeIfAbsent(key, s -> {
            if (priceStreams.size() >= MAX_SYMBOLS) {
                LOGGER.warn("Max symbols limit reached ({}). Consider cleanup.", MAX_SYMBOLS);
                cleanupInactivePriceStreams();
            }
            
            LOGGER.info("Creating new price stream instance for {}: {}", exchangeName, symbol);
            SinglePriceService priceService = new SinglePriceService(exchangeService, exchangeName, symbol);
            priceService.start();
            return priceService;
        });
    }
    
    /**
     * Cleanup inactive price streams to free memory
     */
    private void cleanupInactivePriceStreams() {
        long now = System.currentTimeMillis();
        priceStreams.entrySet().removeIf(entry -> {
            if (now - entry.getValue().getLastAccessTime() > INACTIVE_TIMEOUT_MS) {
                LOGGER.info("Cleaning up inactive price stream: {}", entry.getKey());
                entry.getValue().stop();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Background task to periodically cleanup inactive price streams
     */
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Check every minute
                    cleanupInactivePriceStreams();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("PriceStream-Cleanup");
        cleanupThread.start();
    }
    
    /**
     * Get statistics about active price streams
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "activeSymbols", priceStreams.size(),
            "maxSymbols", MAX_SYMBOLS,
            "symbols", priceStreams.keySet()
        );
    }
}
