package org.ltk.model.exchange.depth;

import java.util.List;

public class Depth {
    private long lastUpdateId;
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
}
