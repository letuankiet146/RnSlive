package org.tk.spot.connector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tk.spot.connector.client.ExchangeName;
import org.tk.spot.connector.dto.PriceDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicLong;

public class SingleSpotPriceService {

    private static final Logger log = LoggerFactory.getLogger(SingleSpotPriceService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final SpotExchangeService spotExchangeService;
    private final ExchangeName exchangeName;
    private final String symbol;
    private final Sinks.Many<PriceDto> sink;
    private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

    private volatile boolean running = false;

    public SingleSpotPriceService(SpotExchangeService spotExchangeService, ExchangeName exchangeName, String symbol) {
        this.spotExchangeService = spotExchangeService;
        this.exchangeName = exchangeName;
        this.symbol = symbol;
        this.sink = Sinks.many().replay().latest();
    }

    public void start() {
        if (running) return;
        running = true;

        spotExchangeService.subscribeMarkPrice(exchangeName, symbol, json -> {
            if (!running) return;
            try {
                PriceDto dto = parsePrice(exchangeName, json);
                if (dto != null) sink.tryEmitNext(dto);
            } catch (Exception e) {
                log.debug("Failed parsing spot price for {} {}: {}", exchangeName, symbol, e.toString());
            }
        });
    }

    public void stop() {
        running = false;
        try {
            spotExchangeService.unsubscribeMarkPrice(exchangeName, symbol);
        } catch (Exception ignored) {
        }
        sink.tryEmitComplete();
    }

    public Flux<PriceDto> getPriceStream() {
        lastAccessTime.set(System.currentTimeMillis());
        return sink.asFlux();
    }

    public long getLastAccessTime() {
        return lastAccessTime.get();
    }

    private PriceDto parsePrice(ExchangeName exchangeName, String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        double price = -1;
        long ts = System.currentTimeMillis();

        switch (exchangeName) {
            case BINANCE -> {
                // Spot @ticker: "c" = last price, "E" = event time
                JsonNode e = root.get("E");
                if (e != null) ts = e.asLong();
                JsonNode c = root.get("c");
                if (c != null) price = c.asDouble();
            }
            case OKX -> {
                // OKX tickers channel: data[0].last, ts
                JsonNode data = root.get("data");
                if (data != null && data.isArray() && !data.isEmpty()) {
                    JsonNode item = data.get(0);
                    JsonNode t = item.get("ts");
                    if (t != null) ts = t.asLong();
                    JsonNode last = item.get("last");
                    if (last != null) price = last.asDouble();
                }
            }
            default -> {
                return null;
            }
        }

        if (price <= 0) return null;
        double top = price * 1.2;
        double bottom = price * 0.8;
        return new PriceDto(top, price, bottom, ts);
    }
}

