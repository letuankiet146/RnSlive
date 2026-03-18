package org.tk.spot.connector.symbol;

import org.tk.spot.connector.client.ExchangeName;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Placeholder for exchanges not yet wired for spot ExchangeInfo REST.
 */
public class PlaceholderSpotExchangeInfoClient implements SpotExchangeInfoClient {

    private final ExchangeName exchange;

    public PlaceholderSpotExchangeInfoClient(ExchangeName exchange) {
        this.exchange = exchange;
    }

    @Override
    public ExchangeName exchange() {
        return exchange;
    }

    @Override
    public Mono<Map<String, String>> fetchBaseToSymbol() {
        return Mono.just(Map.of());
    }
}

