package org.tk.spot.connector.client;

import java.util.function.Consumer;

/**
 * Spot connector interface. Delivers mark/spot price updates as raw String (JSON)
 * via WebSocket, following each exchange's official API.
 */
public interface SpotMarkPriceClient {

    /**
     * Subscribe to mark/spot price stream for the given symbol.
     * Each update is passed to the callback as a String (exchange-specific JSON).
     * Re-subscribing the same symbol closes the previous connection first.
     *
     * @param symbol   exchange-specific symbol (e.g. Binance: btcusdt, OKX: BTC-USDT)
     * @param callback receives each message as String
     */
    void subscribeMarkPrice(String symbol, Consumer<String> callback);

    /**
     * Stop the mark/spot price stream for the given symbol. No-op if not subscribed.
     * Use the same symbol format as for {@link #subscribeMarkPrice}.
     */
    void disconnect(String symbol);

    /**
     * Subscribe to order book depth stream for the given symbol.
     * Each update is passed to the callback as a String (exchange-specific JSON).
     * Re-subscribing the same symbol closes the previous connection first.
     *
     * @param symbol   exchange-specific symbol
     * @param callback receives each message as String
     */
    void subscribeDepth(String symbol, Consumer<String> callback);

    /**
     * Stop the depth stream for the given symbol. No-op if not subscribed.
     */
    void disconnectDepth(String symbol);
}
