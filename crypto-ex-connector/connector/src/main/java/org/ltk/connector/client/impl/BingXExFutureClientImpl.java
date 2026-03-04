package org.ltk.connector.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.ExFutureClient;
import org.ltk.connector.client.RESTApiUrl;
import org.ltk.connector.requestor.api.BingXFutureRequester;
import org.ltk.connector.requestor.ws.BingXFutureWebSocket;
import org.ltk.connector.utils.ConnectorConst;
import org.ltk.connector.utils.HashAlgorithm;
import org.ltk.connector.utils.SecurityHelper;
import org.ltk.connector.utils.UrlBuilder;
import org.ltk.model.AccountKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class BingXExFutureClientImpl implements ExFutureClient {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private BingXFutureRequester requester;

    @Autowired
    private BingXFutureWebSocket webSocket;

    @Autowired
    private AccountKey accountKey;

    @Override
    public void setLeverage(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_SET_LEVERAGE_URL), sortedParams);
        requester.sendRequest(HttpMethod.POST, path, accountKey.getApiKey());
    }

    @Override
    public String getPremiumIndex(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_PREMIUM_INDEX_URL), sortedParams);
        return requester.sendRequest(HttpMethod.GET, path, accountKey.getApiKey());
    }

    private void signedParams(TreeMap<String, Object> sortedParams) {
        sortedParams.put("recvWindow", ConnectorConst.DEFAULT_RECV_WINDOW);
        sortedParams.put("timestamp", System.currentTimeMillis());
        String queryParam = UrlBuilder.joinQueryParameters("", sortedParams);
        String signed = SecurityHelper.hmac(HashAlgorithm.HMAC_SHA256, queryParam, accountKey.getSecretKey());
        sortedParams.put("signature", signed);
    }

    @Override
    public String placeOrder(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String body;
        try {
            body = mapper.writeValueAsString(sortedParams);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return requester.sendRequest(HttpMethod.POST, RESTApiUrl.BINGX_PLACE_ORDER_URL, accountKey.getApiKey(), body);
    }

    @Override
    public String placeMultiOrder(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String body;
        try {
            body = mapper.writeValueAsString(sortedParams);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return requester.sendRequest(HttpMethod.POST, RESTApiUrl.BINGX_PLACE_MULTI_ORDER_URL, accountKey.getApiKey(), body);
    }

    @Override
    public String deleteOrder(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_PLACE_ORDER_URL), sortedParams);
        return requester.sendRequest(HttpMethod.DELETE, path, accountKey.getApiKey());
    }

    @Override
    public String deleteMultiOrder(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_PLACE_MULTI_ORDER_URL), sortedParams);
        return requester.sendRequest(HttpMethod.DELETE, path, accountKey.getApiKey());
    }

    @Override
    public String getOpenOrders(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_QUERY_ALL_OPEN_ORDERS_URL), sortedParams);
        return requester.sendRequest(HttpMethod.GET, path, accountKey.getApiKey());
    }

    @Override
    public String cancelAllOpenOrders(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_CANCEL_ALL_OPEN_ORDERS_URL), sortedParams);
        return requester.sendRequest(HttpMethod.DELETE, path, accountKey.getApiKey());
    }

    @Override
    public String getPosition(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_POSITION_URL), sortedParams);
        return requester.sendRequest(HttpMethod.GET, path, accountKey.getApiKey());
    }
    @Override
    public String getPositionHistory(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_POSITION_HISTORY_URL), sortedParams);
        return requester.sendRequest(HttpMethod.GET, path, accountKey.getApiKey());
    }

    @Override
    public String closeAllPosition(TreeMap<String, Object> sortedParams) {
        signedParams(sortedParams);
        String path = UrlBuilder.joinQueryParameters(String.format("%s?", RESTApiUrl.BINGX_CLOSE_ALL_POSITION_URL), sortedParams);
        return requester.sendRequest(HttpMethod.POST, path, accountKey.getApiKey());
    }

    @Override
    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        String dataType = String.format("%s@lastPrice", symbol);
        String subscribeMessage = String.format("{\"id\": \"id1\", \"reqType\": \"sub\", \"dataType\": \"%s\"}", dataType);
        webSocket.subscribe(subscribeMessage, callback);
    }

    @Override
    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        //BTC-USDT@depth100@500ms.
        String dataType = String.format("%s@depth100@%s", symbol, interval);
        String subscribeMessage = String.format("{\"id\": \"id1\", \"reqType\": \"sub\", \"dataType\": \"%s\"}", dataType);
        webSocket.subscribe(subscribeMessage, callback);
    }

    @Override
    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        String dataType = String.format("%s@trade", symbol);
        String subscribeMessage = String.format("{\"id\": \"id1\", \"reqType\": \"sub\", \"dataType\": \"%s\"}", dataType);
        webSocket.subscribe(subscribeMessage, callback);
    }

    @Override
    public void subscribeAccountData(Consumer<String> callback) {
        webSocket.subscribeAccountData(accountKey.getApiKey(), null, callback);
    }
}
