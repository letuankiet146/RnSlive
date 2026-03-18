package org.tk.spot.connector.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.tk.spot.connector.client.ExchangeName;
import org.tk.spot.connector.client.SpotApiUrls;
import org.tk.spot.connector.dto.PriceDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SpotPriceManager {

    private static final Logger log = LoggerFactory.getLogger(SpotPriceManager.class);

    private static final int MAX_SYMBOLS = 100;
    private static final long INACTIVE_TIMEOUT_MS = 300_000;

    private final SpotExchangeService spotExchangeService;
    private final WebClient webClientBinance;
    private final WebClient webClientOkx;

    private final Map<String, SingleSpotPriceService> streams = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "spot-price-cleaner");
        t.setDaemon(true);
        return t;
    });

    public SpotPriceManager(SpotExchangeService spotExchangeService, WebClient.Builder webClientBuilder) {
        this.spotExchangeService = spotExchangeService;
        this.webClientBinance = webClientBuilder.baseUrl(SpotApiUrls.BINANCE_SPOT_REST).build();
        this.webClientOkx = webClientBuilder.baseUrl(SpotApiUrls.OKX_REST).build();
        cleanupExecutor.scheduleAtFixedRate(this::cleanupInactive, 60, 60, TimeUnit.SECONDS);
    }

    public Flux<PriceDto> getPriceStream(String symbol) {
        return getPriceStream(ExchangeName.BINANCE, symbol);
    }

    public Flux<PriceDto> getPriceStream(ExchangeName exchangeName, String symbol) {
        return getOrCreate(exchangeName, symbol).getPriceStream();
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "activeStreams", streams.size(),
                "maxStreams", MAX_SYMBOLS
        );
    }

    public Mono<JsonNode> getKline(String symbol, String interval) {
        return getKline(ExchangeName.BINANCE, symbol, interval);
    }

    public Mono<JsonNode> getKline(ExchangeName exchangeName, String symbol, String interval) {
        return switch (exchangeName) {
            case BINANCE -> webClientBinance.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/klines")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class);
            case OKX -> webClientOkx.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v5/market/candles")
                            .queryParam("instId", symbol)
                            .queryParam("bar", okxBar(interval))
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class);
            default -> Mono.just(null);
        };
    }

    private String okxBar(String interval) {
        if (interval == null || interval.isBlank()) return "1H";
        String v = interval.trim();
        if (v.endsWith("h")) return v.substring(0, v.length() - 1) + "H";
        if (v.endsWith("d")) return v.substring(0, v.length() - 1) + "D";
        if (v.endsWith("w")) return v.substring(0, v.length() - 1) + "W";
        if (v.endsWith("M")) return v; // already OKX style
        return v;
    }

    private SingleSpotPriceService getOrCreate(ExchangeName exchangeName, String symbol) {
        String key = exchangeName.name() + ":" + symbol;
        return streams.computeIfAbsent(key, k -> {
            if (streams.size() >= MAX_SYMBOLS) cleanupInactive();
            log.info("Creating spot price stream {} {}", exchangeName, symbol);
            SingleSpotPriceService svc = new SingleSpotPriceService(spotExchangeService, exchangeName, symbol);
            svc.start();
            return svc;
        });
    }

    private void cleanupInactive() {
        long now = System.currentTimeMillis();
        streams.entrySet().removeIf(e -> {
            if (now - e.getValue().getLastAccessTime() > INACTIVE_TIMEOUT_MS) {
                log.info("Cleaning inactive spot price stream {}", e.getKey());
                e.getValue().stop();
                return true;
            }
            return false;
        });
    }
}

