package org.tk.rnslive.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tk.rnslive.dto.PriceDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Single price stream instance for one symbol
 * Isolated state management for thread safety and performance
 */
public class SinglePriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SinglePriceService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExchangeService exchangeService;
    private final ExchangeName exchangeName;
    
    // Symbol-specific state
    private final String symbol;
    private final Sinks.Many<PriceDto> priceSink;
    private final AtomicLong lastAccessTime;
    
    // State flags
    private volatile boolean isRunning = false;
    
    public SinglePriceService(ExchangeService exchangeService, ExchangeName exchangeName, String symbol) {
        this.exchangeService = exchangeService;
        this.exchangeName = exchangeName;
        this.symbol = symbol;
        this.priceSink = Sinks.many().replay().latest();
        this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
    }

    public void start() {
        if (isRunning) {
            LOGGER.warn("Price stream for {} is already running", symbol);
            return;
        }
        
        isRunning = true;
        LOGGER.info("Starting price stream for {}", symbol);

        exchangeService.subscribeMarkPrice(exchangeName, symbol, "1s", result -> {
            if (!isRunning) return;
            
            try {
                PriceDto dto = parsePrice(exchangeName, result);
                if (dto != null) {
                    Sinks.EmitResult emitResult = priceSink.tryEmitNext(dto);
                    if (emitResult.isFailure()) {
                        LOGGER.debug("Price could not emit data for {}. Due to {}", symbol, emitResult);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error streaming price for {}", symbol, e);
            }
        });
    }

    public void stop() {
        isRunning = false;
        priceSink.tryEmitComplete();
        LOGGER.info("Stopped price stream for {}", symbol);
    }

    public Flux<PriceDto> getPriceStream() {
        updateLastAccessTime();
        return priceSink.asFlux();
    }

    public long getLastAccessTime() {
        return lastAccessTime.get();
    }

    private void updateLastAccessTime() {
        lastAccessTime.set(System.currentTimeMillis());
    }

        /**
     * Parse price data from exchange response
     */
    private PriceDto parsePrice(ExchangeName exchangeName, String jsonResult) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(jsonResult);
        double currentPrice = -1;
        long timestamp = -1;
        
        switch (exchangeName) {
            case BINANCE -> {
                timestamp = Long.parseLong(rootNode.get("E").asText());
                currentPrice = Double.parseDouble(rootNode.get("p").asText());
            }
            case OKX -> {
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null && dataNode.isArray() && !dataNode.isEmpty()) {
                    timestamp = dataNode.get(0).get("ts").asLong();
                    currentPrice = dataNode.get(0).get("markPx").asDouble();
                }
            }
        }
        
        if (currentPrice < 0) {
            return null;
        }
        
        // Calculate support and resistance levels
        double resistance = currentPrice * 1.2;
        double support = currentPrice * 0.8;
        
        return new PriceDto(resistance, currentPrice, support, timestamp);
    }
}
