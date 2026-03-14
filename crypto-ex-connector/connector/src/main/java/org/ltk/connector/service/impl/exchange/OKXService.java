package org.ltk.connector.service.impl.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.impl.OKXExFutureClientImpl;
import org.ltk.model.exchange.depth.Depth;
import org.ltk.model.exchange.depth.OKXDepth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class OKXService {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OKXExFutureClientImpl exFutureClient;

    public Mono<String> getExchangeInfo() {
        return exFutureClient.getExchangeInfo();
    }

    public Mono<Depth> getDepth(String symbol, int limit) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("instId", symbol);
        sortedParams.put("sz", limit);
        return exFutureClient.getDepth(sortedParams)
            .map(response -> {
                try {
                    String dataArrayJson = mapper.readTree(response).path("data").get(0).toString();
                    return mapper.readValue(dataArrayJson, OKXDepth.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeDepth(symbol, interval, callback);
    }

    public void disconnectDepth(String symbol) {
        exFutureClient.disconnectDepth(symbol);
    }

    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        exFutureClient.subscribeMarkPrice(symbol, null, callback);
    }
}
