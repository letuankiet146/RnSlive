package org.tk.spot.connector.client.impl;

import org.springframework.stereotype.Service;
import org.tk.spot.connector.client.SpotApiUrls;
import org.tk.spot.connector.client.SpotMarkPriceClient;
import org.tk.spot.connector.ws.SpotWebSocketSupport;

import java.util.function.Consumer;

/**
 * Binance Spot WebSocket client.
 * Official docs: https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams
 * Stream: Individual Symbol Ticker — stream name &lt;symbol&gt;@ticker (e.g. btcusdt@ticker).
 * URL: wss://stream.binance.com:9443/ws/&lt;symbol&gt;@ticker
 */
@Service
public class BinanceSpotClientImpl implements SpotMarkPriceClient {

    private final SpotWebSocketSupport webSocketSupport;

    public BinanceSpotClientImpl(SpotWebSocketSupport webSocketSupport) {
        this.webSocketSupport = webSocketSupport;
    }

    private static String subscriptionKey(String symbol) {
        return "BINANCE:" + symbol.strip().toLowerCase();
    }

    private static String depthKey(String symbol) {
        return "BINANCE:" + symbol.strip().toLowerCase() + ":depth";
    }

    @Override
    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        String normalized = symbol.strip().toLowerCase();
        String key = subscriptionKey(symbol);
        String path = normalized + "@ticker";
        String uri = SpotApiUrls.BINANCE_SPOT_WS + "/" + path;
        webSocketSupport.connect(key, uri, null, callback);
    }

    @Override
    public void disconnect(String symbol) {
        webSocketSupport.disconnect(subscriptionKey(symbol));
    }

    @Override
    public void subscribeDepth(String symbol, Consumer<String> callback) {
        String normalized = symbol.strip().toLowerCase();
        String path = normalized + "@depth";
        String uri = SpotApiUrls.BINANCE_SPOT_WS + "/" + path;
        webSocketSupport.connect(depthKey(symbol), uri, null, callback);
    }

    @Override
    public void disconnectDepth(String symbol) {
        webSocketSupport.disconnect(depthKey(symbol));
    }
}
