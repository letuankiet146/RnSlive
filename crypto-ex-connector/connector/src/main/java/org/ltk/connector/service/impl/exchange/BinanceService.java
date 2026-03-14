package org.ltk.connector.service.impl.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.impl.BinanceExFutureClientImpl;
import org.ltk.connector.component.Kline;
import org.ltk.model.exchange.depth.BinanceDepth;
import org.ltk.model.exchange.depth.Depth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class BinanceService {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BinanceExFutureClientImpl exFutureClient;

    public Mono<String> getExchangeInfo() {
        return exFutureClient.getExchangeInfo();
    }

    public Mono<Depth> getDepth(String symbol, int limit) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("limit", limit);
        return exFutureClient.getDepth(sortedParams)
            .map(response -> {
                try {
                    return mapper.readValue(response, BinanceDepth.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public Mono<List<Kline>> getKline(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("interval", interval);
        sortedParams.put("startTime", startTime);
        sortedParams.put("endTime", endTime);
        sortedParams.put("limit", limit);
        return exFutureClient.getKline(sortedParams)
            .map(json -> {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(json);

                    List<Kline> candles = new ArrayList<>();

                    for (JsonNode arr : root) {
                        Kline candle = new Kline();
                        candle.setOpenTime(arr.get(0).asLong());
                        candle.setOpenPrice(new BigDecimal(arr.get(1).asText()));
                        candle.setHighPrice(new BigDecimal(arr.get(2).asText()));
                        candle.setLowPrice(new BigDecimal(arr.get(3).asText()));
                        candle.setClosePrice(new BigDecimal(arr.get(4).asText()));
                        candle.setCloseTime(arr.get(6).asLong());
                        candles.add(candle);
                    }
                    return candles;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeTradeDetail(symbol, interval, callback);
    }

    public void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeMarkPrice(symbol, interval, callback);
    }

    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeDepth(symbol, interval, callback);
    }

    public void subscribeForceOrder(String symbol, Consumer<String> callback) {
        exFutureClient.forceOrder(symbol, callback);
    }
}
