package org.ltk.connector.service.impl.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.impl.BinanceExFutureClientImpl;
import org.ltk.model.exchange.depth.BinanceDepth;
import org.ltk.model.exchange.depth.Depth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class BinanceService {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BinanceExFutureClientImpl exFutureClient;

    public Depth getDepth(String symbol, int limit) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("limit", limit);
        String response = exFutureClient.getDepth(sortedParams);
        try {
            return mapper.readValue(response, BinanceDepth.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeTradeDetail(symbol, interval, callback);
    }

    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeMarkPrice(symbol, interval, callback);
    }

    public void subscribeForceOrder(String symbol, Consumer<String> callback) {
        exFutureClient.forceOrder(symbol, callback);
    }
}
