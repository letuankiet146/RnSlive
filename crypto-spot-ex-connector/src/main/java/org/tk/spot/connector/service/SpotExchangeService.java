package org.tk.spot.connector.service;

import org.tk.spot.connector.client.ExchangeName;

import java.util.function.Consumer;

/**
 * Service to subscribe to spot/mark price by exchange name and symbol.
 * Responses are delivered as String (exchange-specific JSON) via the callback.
 */
public interface SpotExchangeService {

    /**
     * Subscribe to mark/spot price for the given exchange and symbol.
     * Each WebSocket message is passed to the callback as a String.
     * Re-subscribing the same exchange+symbol closes the previous connection first.
     *
     * @param exchangeName BINANCE, OKX, BINGX, or BITMEX
     * @param symbol       exchange-specific symbol (e.g. Binance: btcusdt, OKX: BTC-USDT)
     * @param callback     receives each message as String
     */
    void subscribeMarkPrice(ExchangeName exchangeName, String symbol, Consumer<String> callback);

    /**
     * Stop the mark/spot price stream for the given exchange and symbol.
     * No-op if that channel is not subscribed. Use the same symbol format as for subscribe.
     */
    void unsubscribeMarkPrice(ExchangeName exchangeName, String symbol);

    /**
     * Subscribe to order book depth for the given exchange and symbol.
     * Each WebSocket message is passed to the callback as a String.
     * Re-subscribing the same exchange+symbol closes the previous connection first.
     */
    void subscribeDepth(ExchangeName exchangeName, String symbol, Consumer<String> callback);

    /**
     * Stop the depth stream for the given exchange and symbol. No-op if not subscribed.
     */
    void unsubscribeDepth(ExchangeName exchangeName, String symbol);
}
