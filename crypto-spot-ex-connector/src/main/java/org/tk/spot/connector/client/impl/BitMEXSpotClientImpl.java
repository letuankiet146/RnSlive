package org.tk.spot.connector.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.tk.spot.connector.client.SpotApiUrls;
import org.tk.spot.connector.client.SpotMarkPriceClient;
import org.tk.spot.connector.ws.SpotWebSocketSupport;

import java.util.function.Consumer;

/**
 * BitMEX WebSocket client for mark price.
 * BitMEX is derivatives-only (no spot); this streams instrument updates which include mark price.
 * Official docs: https://docs.bitmex.com/app/wsAPI
 * Subscribe: { "op": "subscribe", "args": [ "instrument:&lt;symbol&gt;" ] } (e.g. instrument:XBTUSD)
 */
@Service
public class BitMEXSpotClientImpl implements SpotMarkPriceClient {

    private final SpotWebSocketSupport webSocketSupport;
    private final ObjectMapper objectMapper;

    public BitMEXSpotClientImpl(SpotWebSocketSupport webSocketSupport, ObjectMapper objectMapper) {
        this.webSocketSupport = webSocketSupport;
        this.objectMapper = objectMapper;
    }

    private static String normalizeSymbol(String symbol) {
        String sym = symbol.strip();
        if (!sym.endsWith("USD") && !sym.contains("USD")) {
            if (sym.equalsIgnoreCase("BTC") || sym.equalsIgnoreCase("XBT")) {
                sym = "XBTUSD";
            } else if (sym.equalsIgnoreCase("ETH")) {
                sym = "ETHUSD";
            } else {
                sym = sym + "USD";
            }
        }
        return sym;
    }

    private static String subscriptionKey(String symbol) {
        return "BITMEX:" + normalizeSymbol(symbol);
    }

    private static String depthKey(String symbol) {
        return "BITMEX:" + normalizeSymbol(symbol) + ":depth";
    }

    @Override
    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        String sym = normalizeSymbol(symbol);
        String arg = "instrument:" + sym;
        ObjectNode root = objectMapper.createObjectNode();
        root.put("op", "subscribe");
        ArrayNode args = objectMapper.createArrayNode();
        args.add(arg);
        root.set("args", args);
        String subscribeMessage;
        try {
            subscribeMessage = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to build subscribe message for symbol: " + symbol, e);
        }
        webSocketSupport.connect(subscriptionKey(symbol), SpotApiUrls.BITMEX_WS, subscribeMessage, callback);
    }

    @Override
    public void disconnect(String symbol) {
        webSocketSupport.disconnect(subscriptionKey(symbol));
    }

    @Override
    public void subscribeDepth(String symbol, Consumer<String> callback) {
        String sym = normalizeSymbol(symbol);
        String arg = "orderBook10:" + sym;
        ObjectNode root = objectMapper.createObjectNode();
        root.put("op", "subscribe");
        ArrayNode args = objectMapper.createArrayNode();
        args.add(arg);
        root.set("args", args);
        String subscribeMessage;
        try {
            subscribeMessage = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to build subscribe message for depth: " + symbol, e);
        }
        webSocketSupport.connect(depthKey(symbol), SpotApiUrls.BITMEX_WS, subscribeMessage, callback);
    }

    @Override
    public void disconnectDepth(String symbol) {
        webSocketSupport.disconnect(depthKey(symbol));
    }
}
