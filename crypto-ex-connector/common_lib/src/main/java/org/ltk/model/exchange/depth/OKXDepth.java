package org.ltk.model.exchange.depth;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OKXDepth extends Depth {
    @JsonProperty("ts")
    @Override
    public void setLastUpdateId(long lastUpdateId) {
        super.setLastUpdateId(lastUpdateId);
    }
}
