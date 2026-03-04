package org.ltk.connector.client.impl;

import org.ltk.connector.client.ExFutureClient;
import org.ltk.connector.client.RESTApiUrl;
import org.ltk.connector.requestor.api.OKXFutureRequester;
import org.ltk.connector.requestor.ws.OKXFutureWebSocket;
import org.ltk.connector.utils.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

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
    public String getDepth(TreeMap<String, Object> sortedParams) {
        String path = UrlBuilder.joinQueryParameters(RESTApiUrl.OKX_GET_DEPTH_FULL_URL +"?", sortedParams);
        return requester.sendRequest(HttpMethod.GET, path);
    }

    @Override
    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, "mark-price", symbol);
        webSocket.subscribe(subscribeMsg, callback);
    }
}
