package org.tk.rnslive.controller.future;

import org.ltk.connector.client.ExchangeName;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.model.OrderBook;
import org.tk.rnslive.services.OrderBookManager;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/future")
public class OrderBookController {
    private final OrderBookManager orderBookManager;

    public OrderBookController(OrderBookManager orderBookManager) {
        this.orderBookManager = orderBookManager;
    }

    @GetMapping(value = "/binance/stream/orderbook", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderBook> streamOrderBook(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        return orderBookManager.getOrderBookStream(symbol);
    }

    @GetMapping(value = "/okx/stream/orderbook", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderBook> streamOkxOrderBook(@RequestParam(defaultValue = "BTC-USDT-SWAP") String symbol) {
        return orderBookManager.getOrderBookStream(ExchangeName.OKX, symbol);
    }

    @GetMapping(value = "/binance/orderbook/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOrderBookStats() {
        return orderBookManager.getStats();
    }
}
