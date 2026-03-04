package org.tk.rnslive.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tk.rnslive.dto.PriceDto;
import jakarta.annotation.PostConstruct;
import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Random;

@Service
public class PriceService {

    @Autowired
    private ExchangeService exchangeService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceService.class);

//    private final Sinks.Many<PriceDto> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<PriceDto> sink = Sinks.many().replay().latest();

    public Flux<PriceDto> getPriceStream() {
        return sink.asFlux();
    }

    private final ObjectMapper mapper = new ObjectMapper();


    @PostConstruct
    public void startPriceGenerator() {
        exchangeService.subscribeMarkPrice(ExchangeName.BINANCE, "BTCUSDT", "1s", result -> {
            try {
                //{"arg":{"channel":"mark-price","instId":"BTC-USDT-SWAP"},"data":[{"instId":"BTC-USDT-SWAP","instType":"SWAP","markPx":"68456.2","ts":"1772589355466"}]}
//              //  {"e":"markPriceUpdate","E":1772614762000,"s":"BTCUSDT","p":"70978.80000000","P":"70070.21013587","i":"71001.88369565","r":"0.00004353","T":1772640000000}
                // parse JSON string
                JsonNode rootNode = mapper.readTree(result);
                long timestamp = Long.parseLong(rootNode.get("E").asText());
                double currentPrice = Double.parseDouble(rootNode.get("p").asText());

                double resistance = currentPrice * 1.2;
                double support = currentPrice * 0.8;

                PriceDto dto = new PriceDto(resistance, round(currentPrice), round(support), timestamp);
                Sinks.EmitResult emitResult = sink.tryEmitNext(dto);
                if (emitResult.isFailure()) {
                    LOGGER.info("Could not emit data. Due to " + emitResult);
                }
            } catch (Exception e) {
                LOGGER.error("Error occurs while streaming price.",e.getCause());
            }
        });
    }
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}