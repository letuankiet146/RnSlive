package org.tk.rnslive.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.model.OrderBook;
import org.tk.rnslive.services.OrderBookService;
import reactor.core.publisher.Flux;

@RestController
public class OrderBookController {
    private final OrderBookService orderBookService;

    public OrderBookController(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @GetMapping(value = "/binance/stream/orderbook", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderBook> streamOrderBook() {
        return orderBookService.getOrderBookStream();
    }
}
