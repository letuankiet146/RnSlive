package org.tk.rnslive.controller;

import org.ltk.connector.component.Kline;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.dto.PriceDto;
import org.tk.rnslive.services.PriceManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class PriceController {
    private final PriceManager priceManager;

    public PriceController(PriceManager priceManager) {
        this.priceManager = priceManager;
    }

    @GetMapping(value = "/binance/stream/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PriceDto> streamPrices(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        return priceManager.getPriceStream(symbol);
    }

    @GetMapping(value = "/binance/kLines", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Kline>> getKline(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "1h") String interval
    ) {
        return priceManager.getKline(symbol, interval);
    }

    @GetMapping(value = "/binance/prices/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getPriceStats() {
        return priceManager.getStats();
    }
}
