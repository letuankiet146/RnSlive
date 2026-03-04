package org.ltk.connector.service.impl.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.impl.OKXExFutureClientImpl;
import org.ltk.model.exchange.depth.Depth;
import org.ltk.model.exchange.depth.OKXDepth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class OKXService {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OKXExFutureClientImpl exFutureClient;

    public Depth getDepth(String symbol, int limit) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("instId", symbol);
        sortedParams.put("sz", limit);
        String response = exFutureClient.getDepth(sortedParams);
        try {
            String dataArrayJson = mapper.readTree(response).path("data").get(0).toString();
            return mapper.readValue(dataArrayJson, OKXDepth.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        exFutureClient.subscribeMarkPrice(symbol, null, callback);
    }
}
