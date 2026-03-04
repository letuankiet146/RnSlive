package org.ltk.connector.client.impl;

import org.ltk.connector.client.ExFutureClient;
import org.ltk.connector.client.RESTApiUrl;
import org.ltk.connector.requestor.api.BinanceFutureRequester;
import org.ltk.connector.requestor.ws.BinanceFutureWebSocket;
import org.ltk.connector.utils.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class BinanceExFutureClientImpl implements ExFutureClient {

    @Autowired
    private BinanceFutureWebSocket futureWebSocket;

    @Autowired
    private BinanceFutureRequester requester;

    @Override
    public String getDepth(TreeMap<String, Object> sortedParams) {
        String path = UrlBuilder.joinQueryParameters(RESTApiUrl.BINANCE_GET_DEPTH_URL +"?", sortedParams);
        return requester.sendRequest(HttpMethod.GET, path);
    }

    @Override
    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        String path = String.format("%s@aggTrade", symbol.toLowerCase());
        futureWebSocket.subscribe(path, callback);
    }

    @Override
    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        String path = String.format("%s@markPrice@%s", symbol.toLowerCase(), interval);
        futureWebSocket.subscribe(path, callback);
    }

    @Override
    public void forceOrder(String symbol, Consumer<String> callback) {
        String path = String.format("%s@forceOrder", symbol.toLowerCase());
        futureWebSocket.subscribe(path, callback);
    }
}
