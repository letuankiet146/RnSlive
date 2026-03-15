package org.tk.spot.connector.service.impl;

import org.springframework.stereotype.Service;
import org.tk.spot.connector.client.ExchangeName;
import org.tk.spot.connector.client.SpotMarkPriceClient;
import org.tk.spot.connector.client.impl.BinanceSpotClientImpl;
import org.tk.spot.connector.client.impl.BingXSpotClientImpl;
import org.tk.spot.connector.client.impl.BitMEXSpotClientImpl;
import org.tk.spot.connector.client.impl.OKXSpotClientImpl;
import org.tk.spot.connector.service.SpotExchangeService;

import java.util.function.Consumer;

@Service
public class SpotExchangeServiceImpl implements SpotExchangeService {

    private final BinanceSpotClientImpl binance;
    private final OKXSpotClientImpl okx;
    private final BingXSpotClientImpl bingx;
    private final BitMEXSpotClientImpl bitmex;

    public SpotExchangeServiceImpl(BinanceSpotClientImpl binance,
                                   OKXSpotClientImpl okx,
                                   BingXSpotClientImpl bingx,
                                   BitMEXSpotClientImpl bitmex) {
        this.binance = binance;
        this.okx = okx;
        this.bingx = bingx;
        this.bitmex = bitmex;
    }

    @Override
    public void subscribeMarkPrice(ExchangeName exchangeName, String symbol, Consumer<String> callback) {
        clientFor(exchangeName).subscribeMarkPrice(symbol, callback);
    }

    @Override
    public void unsubscribeMarkPrice(ExchangeName exchangeName, String symbol) {
        clientFor(exchangeName).disconnect(symbol);
    }

    @Override
    public void subscribeDepth(ExchangeName exchangeName, String symbol, Consumer<String> callback) {
        clientFor(exchangeName).subscribeDepth(symbol, callback);
    }

    @Override
    public void unsubscribeDepth(ExchangeName exchangeName, String symbol) {
        clientFor(exchangeName).disconnectDepth(symbol);
    }

    private SpotMarkPriceClient clientFor(ExchangeName exchangeName) {
        return switch (exchangeName) {
            case BINANCE -> binance;
            case OKX -> okx;
            case BINGX -> bingx;
            case BITMEX -> bitmex;
        };
    }
}
