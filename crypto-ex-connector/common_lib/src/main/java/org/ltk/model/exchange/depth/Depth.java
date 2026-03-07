package org.ltk.model.exchange.depth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Depth {
    private long lastUpdateId;
    @JsonProperty("T")
    private long transactionTime;
    private List<List<Double>> bids; // PRICE : QTY
    private List<List<Double>> asks; // PRICE : QTY

    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    public List<List<Double>> getBids() {
        return bids;
    }

    public void setBids(List<List<Double>> bids) {
        this.bids = bids;
    }

    public List<List<Double>> getAsks() {
        return asks;
    }

    public void setAsks(List<List<Double>> asks) {
        this.asks = asks;
    }

    public long getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(long transactionTime) {
        this.transactionTime = transactionTime;
    }
}
