package org.ltk.connector.client.impl;

import org.ltk.connector.client.ExFutureClient;
import org.ltk.connector.requestor.ws.BybitFutureWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class BybitExFutureClientImpl implements ExFutureClient {
    public static final String SUBSCRIBE_MSG_FORMAT = "{\"req_id\": \"test\",\"op\": \"subscribe\",\"args\": [\"%s\"]}";

    @Autowired
    private BybitFutureWebSocket futureWebSocket;

    @Override
    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, "publicTrade."+symbol);
        futureWebSocket.subscribe(subscribeMsg, callback);
    }

    @Override
    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        String subscribeMsg = String.format(SUBSCRIBE_MSG_FORMAT, "orderbook."+interval+"."+symbol);
        futureWebSocket.subscribe(subscribeMsg, callback);
    }
}
