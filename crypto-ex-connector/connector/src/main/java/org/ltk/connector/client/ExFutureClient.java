package org.ltk.connector.client;

import reactor.core.publisher.Mono;

import java.util.TreeMap;
import java.util.function.Consumer;

public interface ExFutureClient {
    default Mono<Void> setLeverage(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getPremiumIndex(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> placeOrder(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> placeMultiOrder(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> deleteOrder(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> deleteMultiOrder(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getOpenOrders(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> cancelAllOpenOrders(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getPosition(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getPositionHistory(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> closeAllPosition(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getExchangeInfo() {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getDepth(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default Mono<String> getKline(TreeMap<String, Object> sortedParams) {
        return Mono.error(new RuntimeException("Exchange not supported"));
    }
    default void subscribeMarkPrice(String symbol, String interval, Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }
    default void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        throw new RuntimeException("Exchange not supported");
    }

    /**
     * Manually interrupt the depth WebSocket for the given symbol so it can reconnect and send a fresh snapshot.
     * No-op for exchanges that do not support it.
     */
    default void disconnectDepth(String symbol) {
        // no-op by default
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
