package org.tk.rnslive.controller;

import org.ltk.model.exchange.depth.Depth;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.model.OrderBook;
import org.tk.rnslive.services.OrderBookManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class OrderBookController {
    private final OrderBookManager orderBookManager;

    public OrderBookController(OrderBookManager orderBookManager) {
        this.orderBookManager = orderBookManager;
    }

    @GetMapping(value = "/binance/stream/orderbook", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderBook> streamOrderBook(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        return orderBookManager.getOrderBookStream(symbol);
    }

    @GetMapping(value = "/binance/orderbook/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOrderBookStats() {
        return orderBookManager.getStats();
    }
}
