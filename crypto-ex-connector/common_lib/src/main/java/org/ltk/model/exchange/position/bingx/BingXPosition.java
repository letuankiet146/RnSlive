package org.ltk.model.exchange.position.bingx;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ltk.model.exchange.position.Position;

public class BingXPosition extends Position {

    @JsonProperty("positionId")
    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @JsonProperty("positionCommission")
    @Override
    public void setFee(String fee) {
        super.setFee(fee);
    }
}
