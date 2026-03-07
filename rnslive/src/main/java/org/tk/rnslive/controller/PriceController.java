package org.tk.rnslive.controller;

import org.ltk.connector.component.Kline;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.dto.PriceDto;
import org.tk.rnslive.services.PriceService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class PriceController {
    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping(value = "/okx/stream/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PriceDto> streamPrices() {
        return priceService.getPriceStream();
    }

    @GetMapping(value = "/binance/kLines", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Kline>> getKline(
            @RequestParam(defaultValue = "1m") String interval
    ) {
        return priceService.getKline(interval);
    }
}
