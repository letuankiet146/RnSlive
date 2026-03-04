package org.ltk.connector.service.impl;

import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.ltk.connector.service.impl.exchange.BinanceService;
import org.ltk.connector.service.impl.exchange.BingXService;
import org.ltk.connector.service.impl.exchange.BybitService;
import org.ltk.connector.service.impl.exchange.OKXService;
import org.ltk.model.exchange.depth.Depth;
import org.ltk.model.exchange.order.Order;
import org.ltk.model.exchange.order.SideOrder;
import org.ltk.model.exchange.order.bingx.BingXOrder;
import org.ltk.model.exchange.position.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ExchangeServiceImpl implements ExchangeService {
    @Autowired
    private BingXService bingXService;

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private BybitService bybitService;

    @Autowired
    private OKXService okxService;


    @Override
    public List<Order> createMultiOrders(ExchangeName exchangeName, String symbol, Set<Order> orders) {
        switch (exchangeName) {
            case BINGX:
                Set<BingXOrder> bingXOrders = orders.stream().map(order -> (BingXOrder) order).collect(Collectors.toSet());
                return bingXService.createOrders(symbol, bingXOrders);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public Order createLimitOrder(ExchangeName exchangeName, String symbol, SideOrder side, double price, double quantity) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.createLimitOrder(symbol, side, price, quantity);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public Order createMarketOrder(ExchangeName exchangeName, String symbol, SideOrder side, double quantity) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.createMarketOrder(symbol, side, quantity);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public List<Order> getOpenOrders(ExchangeName exchangeName, String symbol) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.getOpenOrders(symbol);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void cancelOrder(ExchangeName exchangeName, String symbol, String orderId) {
        switch (exchangeName) {
            case BINGX:
                bingXService.cancelOrder(symbol, orderId);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }
    @Override
    public void cancelMultiOrder(ExchangeName exchangeName, String symbol, Set<Long> orderIds) {
        switch (exchangeName) {
            case BINGX:
                bingXService.cancelOrder(symbol, orderIds);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void cancelAllOpenOrders(ExchangeName exchangeName, String symbol) {
        switch (exchangeName) {
            case BINGX:
                bingXService.cancelAllOpenOrders(symbol);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public List<Position> getPosition(ExchangeName exchangeName, String symbol) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.getPosition(symbol);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public List<Position> getPositionHistory(ExchangeName exchangeName, String symbol, String positionId) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.getPositionHistory(symbol, positionId);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public Order createTakeProfitOrder(ExchangeName exchangeName, String symbol, SideOrder side, double price, double quantity) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.createTakeProfitOrder(symbol, side, price, quantity);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public Order createStopLossOrder(ExchangeName exchangeName, String symbol, SideOrder side, double price, double quantity) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.createStopLossOrder(symbol, side, price, quantity);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void closeAllPosition(ExchangeName exchangeName, String symbol) {
        switch (exchangeName) {
            case BINGX:
                bingXService.closeAllPosition(symbol);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public double getMarkPrice(ExchangeName exchangeName, String symbol) {
        switch (exchangeName) {
            case BINGX:
                return bingXService.getMarkPrice(symbol);
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void setLeverage(ExchangeName exchangeName, String symbol, int leverage, String positionSide) {
        switch (exchangeName) {
            case BINGX:
                bingXService.setLeverage(symbol, leverage, positionSide);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public Depth getDepth(ExchangeName exchangeName, String symbol, int limit) {
        switch (exchangeName) {
            case BINGX:
                throw new RuntimeException("Not implemented");
            case BINANCE:
                return binanceService.getDepth(symbol, limit);
            case OKX:
                return okxService.getDepth(symbol, limit);
            case BYBIT:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void subscribeMarkPrice(ExchangeName exchangeName, String symbol, String interval, Consumer<String> callback) {
        switch (exchangeName) {
            case BINGX:
                bingXService.subscribeMarkPrice(symbol, callback);
                break;
            case BINANCE:
                binanceService.subscribeMarkPrice(symbol, interval, callback);
                break;
            case OKX:
                okxService.subscribeMarkPrice(symbol, callback);
                break;
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void subscribeDepth(ExchangeName exchangeName, String symbol, String interval, Consumer<String> callback) {
        switch (exchangeName) {
            case BINGX:
                bingXService.subscribeDepth(symbol, interval, callback);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            case BYBIT:
                bybitService.subscribeDepth(symbol, interval, callback);
                break;
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void subscribeTradeDetail(ExchangeName exchangeName, String symbol, String interval, Consumer<String> callback) {
        switch (exchangeName) {
            case BINGX:
                bingXService.subscribeTradeDetail(symbol, interval, callback);
                break;
            case BINANCE:
                binanceService.subscribeTradeDetail(symbol, interval, callback);
                break;
            case BYBIT:
                bybitService.subscribeTradeDetail(symbol, interval, callback);
                break;
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void subscribeAccountData(ExchangeName exchangeName, Consumer<String> callback) {
        switch (exchangeName) {
            case BINGX:
                bingXService.subscribeAccountData(callback);
                break;
            case BINANCE:
                throw new RuntimeException("Not implemented");
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

    @Override
    public void subscribeForceOrder(ExchangeName exchangeName, String symbol, Consumer<String> callback) {
        switch (exchangeName) {
            case BINANCE:
                binanceService.subscribeForceOrder(symbol, callback);
                break;
            default:
                throw new RuntimeException("Exchange not supported: " + exchangeName);
        }
    }

}
