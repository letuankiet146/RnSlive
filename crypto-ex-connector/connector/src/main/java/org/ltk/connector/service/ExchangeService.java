package org.ltk.connector.service;

import org.ltk.connector.client.ExchangeName;
import org.ltk.model.exchange.depth.Depth;
import org.ltk.model.exchange.order.Order;
import org.ltk.model.exchange.order.SideOrder;
import org.ltk.model.exchange.position.Position;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface ExchangeService {
    List<Order> createMultiOrders(ExchangeName exchangeName, String symbol, Set<Order> orders);
    Order createLimitOrder(ExchangeName exchangeName, String symbol, SideOrder side, double price, double quantity);
    Order createMarketOrder(ExchangeName exchangeName, String symbol, SideOrder side, double quantity);
    List<Order> getOpenOrders(ExchangeName exchangeName, String symbol);
    void cancelOrder(ExchangeName exchangeName, String symbol, String orderId);
    void cancelMultiOrder(ExchangeName exchangeName, String symbol, Set<Long> orderIds);
    void cancelAllOpenOrders(ExchangeName exchangeName, String symbol);
    List<Position> getPosition(ExchangeName exchangeName, String symbol);
    List<Position> getPositionHistory(ExchangeName exchangeName, String symbol, String positionId);
    Order createTakeProfitOrder(ExchangeName exchangeName, String symbol, SideOrder side, double price, double quantity);
    Order createStopLossOrder(ExchangeName exchangeName, String symbol, SideOrder side, double price, double quantity);
    void closeAllPosition(ExchangeName exchangeName, String symbol);
    double getMarkPrice(ExchangeName exchangeName, String symbol);
    void setLeverage(ExchangeName exchangeName, String symbol, int leverage, String positionSide);
    Depth getDepth(ExchangeName exchangeName, String symbol, int limit);

    void subscribeMarkPrice(ExchangeName exchangeName, String symbol, String interval, Consumer<String> callback);
    void subscribeDepth(ExchangeName exchangeName, String symbol, String interval, Consumer<String> callback);
    void subscribeTradeDetail(ExchangeName exchangeName, String symbol, String interval, Consumer<String> callback);
    void subscribeAccountData(ExchangeName exchangeName, Consumer<String> callback);
    void subscribeForceOrder(ExchangeName exchangeName, String symbol, Consumer<String> callback);

}
