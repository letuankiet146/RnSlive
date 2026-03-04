package org.ltk.model.exchange.order.bingx;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ltk.model.exchange.order.Order;
import org.ltk.model.exchange.order.OrderStatus;
import org.ltk.model.exchange.order.OrderType;
import org.ltk.model.exchange.order.SideOrder;

public class BingXWSOrder extends Order {

    @Override
    @JsonProperty("i")
    public String getOrderId() {
        return super.getOrderId();
    }

    @Override
    @JsonProperty("i")
    public void setOrderId(String orderId) {
        super.setOrderId(orderId);
    }

    @Override
    @JsonProperty("s")
    public String getSymbol() {
        return super.getSymbol();
    }

    @Override
    @JsonProperty("s")
    public void setSymbol(String symbol) {
        super.setSymbol(symbol);
    }

    @Override
    @JsonProperty("o")
    public OrderType getType() {
        return super.getType();
    }

    @Override
    @JsonProperty("o")
    public void setType(OrderType type) {
        super.setType(type);
    }

    @Override
    @JsonProperty("X")
    public OrderStatus getStatus() {
        return super.getStatus();
    }

    @Override
    @JsonProperty("X")
    public void setStatus(OrderStatus status) {
        super.setStatus(status);
    }

    @Override
    @JsonProperty("S")
    public SideOrder getSide() {
        return super.getSide();
    }

    @Override
    @JsonProperty("S")
    public void setSide(SideOrder side) {
        super.setSide(side);
    }

    @Override
    @JsonProperty("p")
    public double getPrice() {
        return super.getPrice();
    }

    @Override
    @JsonProperty("p")
    public void setPrice(double price) {
        super.setPrice(price);
    }

    @Override
    @JsonProperty("q")
    public double getQuantity() {
        return super.getQuantity();
    }

    @Override
    @JsonProperty("q")
    public void setQuantity(double quantity) {
        super.setQuantity(quantity);
    }
}
