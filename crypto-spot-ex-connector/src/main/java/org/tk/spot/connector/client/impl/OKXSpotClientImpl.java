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
 * OKX Spot WebSocket client.
 * Official docs: https://www.okx.com/docs-v5/en/#websocket-api-public-channel-tickers-channel
 * Channel: tickers, args: [{ "channel": "tickers", "instId": "BTC-USDT" }]
 * Subscribe message: { "op": "subscribe", "args": [ { "channel": "tickers", "instId": "&lt;instId&gt;" } ] }
 */
@Service
public class OKXSpotClientImpl implements SpotMarkPriceClient {

    private final SpotWebSocketSupport webSocketSupport;
    private final ObjectMapper objectMapper;

    public OKXSpotClientImpl(SpotWebSocketSupport webSocketSupport, ObjectMapper objectMapper) {
        this.webSocketSupport = webSocketSupport;
        this.objectMapper = objectMapper;
    }

    private static String normalizeInstId(String symbol) {
        String instId = symbol.strip().toUpperCase();
        if (!instId.contains("-")) {
            if (instId.endsWith("USDT")) {
                instId = instId.substring(0, instId.length() - 4) + "-USDT";
            } else {
                instId = instId + "-USDT";
            }
        }
        return instId;
    }

    private String subscriptionKey(String symbol) {
        return "OKX:" + normalizeInstId(symbol);
    }

    private String depthKey(String symbol) {
        return "OKX:" + normalizeInstId(symbol) + ":depth";
    }

    @Override
    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        String instId = normalizeInstId(symbol);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("op", "subscribe");
        ArrayNode args = objectMapper.createArrayNode();
        ObjectNode arg = objectMapper.createObjectNode();
        arg.put("channel", "tickers");
        arg.put("instId", instId);
        args.add(arg);
        root.set("args", args);
        String subscribeMessage;
        try {
            subscribeMessage = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to build subscribe message for symbol: " + symbol, e);
        }
        webSocketSupport.connect(subscriptionKey(symbol), SpotApiUrls.OKX_PUBLIC_WS, subscribeMessage, callback);
    }

    @Override
    public void disconnect(String symbol) {
        webSocketSupport.disconnect(subscriptionKey(symbol));
    }

    @Override
    public void subscribeDepth(String symbol, Consumer<String> callback) {
        String instId = normalizeInstId(symbol);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("op", "subscribe");
        ArrayNode args = objectMapper.createArrayNode();
        ObjectNode arg = objectMapper.createObjectNode();
        arg.put("channel", "books5");
        arg.put("instId", instId);
        args.add(arg);
        root.set("args", args);
        String subscribeMessage;
        try {
            subscribeMessage = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to build subscribe message for depth: " + symbol, e);
        }
        webSocketSupport.connect(depthKey(symbol), SpotApiUrls.OKX_PUBLIC_WS, subscribeMessage, callback);
    }

    @Override
    public void disconnectDepth(String symbol) {
        webSocketSupport.disconnect(depthKey(symbol));
    }
}
