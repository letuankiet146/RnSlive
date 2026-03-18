package org.tk.spot.connector.symbol;

import org.tk.spot.connector.client.ExchangeName;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface SpotExchangeInfoClient {
    ExchangeName exchange();

    /**
     * Fetch base->symbol mapping for SPOT instruments.
     * Keys must be uppercase base asset (e.g. BTC) and values exchange-native symbols (e.g. BTCUSDT or BTC-USDT).
     */
    Mono<Map<String, String>> fetchBaseToSymbol();
}

