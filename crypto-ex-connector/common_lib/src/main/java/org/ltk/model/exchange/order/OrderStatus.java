package org.ltk.model.exchange.order;

public enum OrderStatus {
    NEW,FILLED,PARTIALLY_FILLED,CANCELED,EXPIRED, // Binance status
    PENDING, WORKING, PARTIALLYFILLED // Bingx status
}
