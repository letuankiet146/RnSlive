package org.tk.spot.connector.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.spot.connector.dto.PriceDto;
import org.tk.spot.connector.service.SpotPriceManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/spot")
public class SpotPriceController {

    private final SpotPriceManager priceManager;

    public SpotPriceController(SpotPriceManager priceManager) {
        this.priceManager = priceManager;
    }

    @GetMapping(value = "/binance/stream/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PriceDto> streamPrices(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        return priceManager.getPriceStream(symbol);
    }

    @GetMapping(value = "/okx/stream/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PriceDto> streamOkxPrices(@RequestParam(defaultValue = "BTC-USDT") String symbol) {
        return priceManager.getPriceStream(org.tk.spot.connector.client.ExchangeName.OKX, symbol);
    }

    @GetMapping(value = "/binance/kLines", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getKline(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "1h") String interval
    ) {
        return priceManager.getKline(symbol, interval);
    }

    @GetMapping(value = "/okx/kLines", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getOkxKline(
            @RequestParam(defaultValue = "BTC-USDT") String symbol,
            @RequestParam(defaultValue = "1H") String interval
    ) {
        return priceManager.getKline(org.tk.spot.connector.client.ExchangeName.OKX, symbol, interval);
    }

    @GetMapping(value = "/binance/prices/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getPriceStats() {
        return priceManager.getStats();
    }
}

