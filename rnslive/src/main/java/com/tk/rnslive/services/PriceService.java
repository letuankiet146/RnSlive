package com.tk.rnslive.services;

import com.tk.rnslive.dto.PriceDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Random;

@Service
public class PriceService {

    private final Sinks.Many<PriceDto> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    private final Random random = new Random();

    private double basePrice = 100;

    public Flux<PriceDto> getPriceStream() {
        return sink.asFlux();
    }

    @PostConstruct
    public void startPriceGenerator() {

        Flux.interval(Duration.ofSeconds(1))
                .map(i -> {

                    double variation = (random.nextDouble() - 0.5) * 2;
                    double currentPrice = basePrice + variation;

                    double resistance = currentPrice + random.nextDouble() * 5 + 2;
                    double support = currentPrice - random.nextDouble() * 5 - 2;

                    basePrice = currentPrice;

                    return new PriceDto(
                            round(resistance),
                            round(currentPrice),
                            round(support)
                    );
                })
                .subscribe(dto -> sink.tryEmitNext(dto));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}