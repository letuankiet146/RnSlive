package org.tk.spot.connector.client.impl;

import org.springframework.stereotype.Service;
import org.tk.spot.connector.client.SpotApiUrls;
import org.tk.spot.connector.client.SpotMarkPriceClient;
import org.tk.spot.connector.ws.SpotWebSocketSupport;

import java.util.function.Consumer;

/**
 * BingX Spot WebSocket client.
 * Pattern aligned with BingX swap API: subscribe with reqType "sub", dataType "&lt;symbol&gt;@lastPrice".
 * Official docs: https://bingx-api.github.io/docs (spot market API).
 * Spot WebSocket base: wss://open-api.bingx.com/market
 */
@Service
public class BingXSpotClientImpl implements SpotMarkPriceClient {

    private final SpotWebSocketSupport webSocketSupport;

    public BingXSpotClientImpl(SpotWebSocketSupport webSocketSupport) {
        this.webSocketSupport = webSocketSupport;
    }

    private static String normalizeSymbol(String symbol) {
        String normalized = symbol.strip().toUpperCase();
        if (!normalized.contains("-")) {
            if (normalized.endsWith("USDT")) {
                normalized = normalized.substring(0, normalized.length() - 4) + "-USDT";
            } else {
                normalized = normalized + "-USDT";
            }
        }
        return normalized;
    }

    private static String subscriptionKey(String symbol) {
        return "BINGX:" + normalizeSymbol(symbol);
    }

    private static String depthKey(String symbol) {
        return "BINGX:" + normalizeSymbol(symbol) + ":depth";
    }

    @Override
    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        String normalized = normalizeSymbol(symbol);
        String dataType = normalized + "@lastPrice";
        String subscribeMessage = String.format("{\"id\": \"spot-mark-1\", \"reqType\": \"sub\", \"dataType\": \"%s\"}", dataType);
        webSocketSupport.connect(subscriptionKey(symbol), SpotApiUrls.BINGX_SPOT_WS, subscribeMessage, callback);
    }

    @Override
    public void disconnect(String symbol) {
        webSocketSupport.disconnect(subscriptionKey(symbol));
    }

    @Override
    public void subscribeDepth(String symbol, Consumer<String> callback) {
        String normalized = normalizeSymbol(symbol);
        String dataType = normalized + "@depth";
        String subscribeMessage = String.format("{\"id\": \"spot-depth-1\", \"reqType\": \"sub\", \"dataType\": \"%s\"}", dataType);
        webSocketSupport.connect(depthKey(symbol), SpotApiUrls.BINGX_SPOT_WS, subscribeMessage, callback);
    }

    @Override
    public void disconnectDepth(String symbol) {
        webSocketSupport.disconnect(depthKey(symbol));
    }
}
