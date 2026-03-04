package org.ltk.connector.service.impl.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.impl.BybitExFutureClientImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class BybitService {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BybitExFutureClientImpl exFutureClient;

    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeTradeDetail(symbol, interval, callback);
    }

    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeDepth(symbol, interval, callback);
    }

    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        exFutureClient.subscribeMarkPrice(symbol, null, callback);
    }
}
