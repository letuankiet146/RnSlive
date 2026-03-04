package org.tk.rnslive.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.dto.PriceDto;
import org.tk.rnslive.services.PriceService;
import reactor.core.publisher.Flux;

@RestController
public class PriceController {
    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping(value = "/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PriceDto> streamPrices() {
        return priceService.getPriceStream();
    }
}