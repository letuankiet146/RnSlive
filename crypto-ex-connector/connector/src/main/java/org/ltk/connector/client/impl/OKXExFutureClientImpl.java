package org.ltk.connector.client.impl;

import org.ltk.connector.client.ExFutureClient;
import org.ltk.connector.client.RESTApiUrl;
import org.ltk.connector.requestor.api.OKXFutureRequester;
import org.ltk.connector.requestor.ws.OKXFutureWebSocket;
import org.ltk.connector.utils.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class OKXExFutureClientImpl implements ExFutureClient {
    private static final String DEPTH_CHANNEL = "books";
    private static final String SUBSCRIBE_MSG_FORMAT = "{\n" +
            "    \"op\":\"subscribe\",\n" +
            "    \"args\":[\n" +
            "        {\n" +
            "            \"channel\":\"%s\",\n" +
            "            \"instId\":\"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";

    @Autowired
    private OKXFutureRequester requester;

    @Autowired
    private OKXFutureWebSocket webSocket;

    @Override
    public Mono<String> getExchangeInfo() {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("instType", "SWAP");
        String path = UrlBuilder.joinQueryParameters(RESTApiUrl.OKX_GET_INSTRUMENTS_URL +"?", sortedParams);
        return requester.sendRequest(HttpMethod.GET, path);
    }

    @Override
    public Mono<String> getDepth(TreeMap<String, Object> sortedParams) {
        String path = UrlBuilder.joinQueryParameters(RESTApiUrl.OKX_GET_DEPTH_FULL_URL +"?", sortedParams);
        return requester.sendRequest(HttpMethod.GET, path);
    }


    @Override
    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        String key = DEPTH_CHANNEL + "_" + symbol;
        String instId = symbol.toUpperCase();
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, DEPTH_CHANNEL, instId);
        webSocket.subscribe(key, subscribeMsg, callback);
    }

    @Override
    public void disconnectDepth(String symbol) {
        if (symbol == null) return;
        String key = DEPTH_CHANNEL + "_" + symbol;
        webSocket.disconnect(key);
    }

    @Override
    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        String instId = symbol.toUpperCase();
        String channel = "mark-price";
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, channel, instId);
        webSocket.subscribe(channel, subscribeMsg, callback);
    }
}
