package org.ltk.connector.client;

import java.util.TreeMap;
import java.util.function.Consumer;

public interface ExFutureClient {
    default void setLeverage(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String getPremiumIndex(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String placeOrder(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String placeMultiOrder(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String deleteOrder(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String deleteMultiOrder(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String getOpenOrders(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String cancelAllOpenOrders(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String getPosition(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String getPositionHistory(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String closeAllPosition(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default String getDepth(TreeMap<String, Object> sortedParams) {
        throw new RuntimeException("Exchange not supported");
    }
    default void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }
    default void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }
    default void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }
    default void subscribeAccountData(Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }
    default void forceOrder(String symbol, Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }
}
