package org.ltk.model.exchange.order.bingx;

import org.ltk.model.exchange.order.Order;
import org.ltk.model.exchange.order.OrderStatus;
import org.ltk.model.exchange.order.OrderType;
import org.ltk.model.exchange.order.SideOrder;

public class BingXOrder extends Order {
    public BingXOrder() {
    }
    public BingXOrder(String symbol, OrderType orderType, SideOrder sideOrder, String positionSide, double price, double quantity) {
        super(symbol, orderType, sideOrder, positionSide, price, quantity);
    }
    @Override
    public OrderStatus getStatus() {
        if(super.getStatus() == null) {
            return OrderStatus.NEW;
        }
        return super.getStatus();
    }
}
