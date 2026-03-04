package org.ltk.model.exchange.depth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BybitDepth extends Depth {
    @JsonProperty("u")
    @Override
    public void setLastUpdateId(long lastUpdateId) {
        super.setLastUpdateId(lastUpdateId);
    }

    @JsonProperty("b")
    @Override
    public void setBids(List<List<Double>> bids) {
        super.setBids(bids);
    }

    @JsonProperty("a")
    @Override
    public void setAsks(List<List<Double>> asks) {
        super.setAsks(asks);
    }


}
