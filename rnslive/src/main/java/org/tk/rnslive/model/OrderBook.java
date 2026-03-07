package org.tk.rnslive.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonPropertyOrder({ "timestamp", "asks", "bids", "lastUpdateId","symbol"  })
public class OrderBook {
    private final String symbol;
    private final long lastUpdateId;
    private final Map<Double, Double> asks; // Price -> Quantity (ascending)
    private final Map<Double, Double> bids; // Price -> Quantity (descending)
    private final long timestamp;

    // Constructor for creating immutable snapshots
    public OrderBook(String symbol, long lastUpdateId, Map<Double, Double> bids, Map<Double, Double> asks, long timestamp) {
        this.symbol = symbol;
        this.lastUpdateId = lastUpdateId;
        this.bids = bids;
        this.asks = asks;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public Map<Double, Double> getBids() {
        return bids;
    }

    public Map<Double, Double> getAsks() {
        return asks;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
