package org.tk.rnslive.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.ltk.connector.component.Kline;
import org.ltk.model.exchange.depth.Depth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tk.rnslive.dto.PriceDto;
import jakarta.annotation.PostConstruct;
import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class PriceService {

    @Autowired
    private ExchangeService exchangeService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Sinks.Many<PriceDto> priceSink = Sinks.many().replay().latest();
    private final Sinks.Many<Depth> depthSink = Sinks.many().replay().latest();

    public Flux<PriceDto> getPriceStream() {
        return priceSink.asFlux();
    }

    public Flux<Depth> getDepthStream() {
        return depthSink.asFlux();
    }

    public Mono<List<Kline>> getKline(String interval) {
        return exchangeService.getKline(ExchangeName.BINANCE,"BTCUSDT", interval, null, null, null);
    }

    private Mono<Depth> getDepthSnapshot(String symbol) {
        return exchangeService.getDepth(ExchangeName.BINANCE,symbol, 1000);
    }

    @PostConstruct
    public void init() {
        startPriceGenerator();
        startDepthGenerator(ExchangeName.BINANCE, "BTCUSDT");
    }

    private Depth getDepth(String json) {
        Depth depth = new Depth();

        try {
            JsonNode root = mapper.readTree(json);

            // Set last update ID
            depth.setLastUpdateId(root.get("u").asLong());

            // Parse bids
            JsonNode bidsNode = root.get("b");
            List<List<Double>> bids = new ArrayList<>();
            for (JsonNode bidNode : bidsNode) {
                List<Double> bid = new ArrayList<>();
                bid.add(bidNode.get(0).asDouble()); // price
                bid.add(bidNode.get(1).asDouble()); // quantity
                bids.add(bid);
            }
            depth.setBids(bids);

            // Parse asks
            JsonNode asksNode = root.get("a");
            List<List<Double>> asks = new ArrayList<>();
            for (JsonNode askNode : asksNode) {
                List<Double> ask = new ArrayList<>();
                ask.add(askNode.get(0).asDouble()); // price
                ask.add(askNode.get(1).asDouble()); // quantity
                asks.add(ask);
            }
            depth.setAsks(asks);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return depth;
    }


    private void startDepthGenerator(ExchangeName exchangeName, String code) {
        exchangeService.subscribeDepth(exchangeName, code, null, result -> {
            try {
                Depth dto = getDepth(result);
                if (dto != null) {
                    Sinks.EmitResult emitResult = depthSink.tryEmitNext(dto);
                    if (emitResult.isFailure()) {
                        LOGGER.info("Depth could not emit data. Due to " + emitResult);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error occurs while streaming price.",e.getCause());
            }
        });
    }

    private PriceDto getPrice(ExchangeName exchangeName, String jsonResult) throws JsonProcessingException {
        PriceDto dto;
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
        double resistance = currentPrice * 1.2;
        double support = currentPrice * 0.8;
        dto = new PriceDto(resistance, round(currentPrice), round(support), timestamp);
        return dto;
    }
    private void startPriceGenerator() {
        final ExchangeName exchangeName = ExchangeName.OKX;
        final String code = "BTC-USDT-SWAP";
        exchangeService.subscribeMarkPrice(exchangeName, code, null, result -> {
            try {
                PriceDto dto = getPrice(exchangeName, result);
                if (dto != null) {
                    Sinks.EmitResult emitResult = priceSink.tryEmitNext(dto);
                    if (emitResult.isFailure()) {
                        LOGGER.info("Price could not emit data. Due to " + emitResult);
                    }
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
