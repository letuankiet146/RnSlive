package org.ltk.model.exchange.order;

public class Order {
    private Integer userId = 1;
    private String orderId;
    private String symbol;
    private OrderType type;
    private OrderStatus status;
    private SideOrder side;
    private String positionSide;
    private double price;
    private double quantity;
    private double stopPrice;
    public Order() {
    }
    public Order(String symbol, OrderType type, SideOrder side, String positionSide, double price, double quantity) {
        this.symbol = symbol;
        this.type = type;
        this.side = side;
        this.positionSide = positionSide;
        this.price = price;
        this.quantity = quantity;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public SideOrder getSide() {
        return side;
    }

    public void setSide(SideOrder side) {
        this.side = side;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
    }
}
