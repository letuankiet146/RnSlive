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
        // Default to books (400 levels, incremental) if no specific channel is provided.
        // If interval is provided, treat it as the desired OKX order book channel name
        // (e.g. "books", "books5", "bbo-tbt", "books-l2-tbt", "books50-l2-tbt").
        String channel = (interval == null || interval.isBlank()) ? "books" : interval;
        String instId = symbol.toUpperCase();
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, channel, instId);
        webSocket.subscribe(subscribeMsg, callback);
    }

    @Override
    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        String instId = symbol.toUpperCase();
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, "mark-price", instId);
        webSocket.subscribe(subscribeMsg, callback);
    }
}
