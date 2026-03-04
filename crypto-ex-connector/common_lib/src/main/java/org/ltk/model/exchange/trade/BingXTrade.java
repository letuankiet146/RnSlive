package org.ltk.model.exchange.trade;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ltk.model.AggregateTrade;

import java.math.BigDecimal;

public class BingXTrade extends AggregateTrade {
    @Override
    @JsonProperty("p")
    public void setPrice(BigDecimal price) {
        super.setPrice(price);
    }

    @Override
    @JsonProperty("q")
    public void setQty(double qty) {
        super.setQty(qty);
    }

    @Override
    @JsonProperty("m")
    public void setBuyerMaker(boolean buyerMaker) {
        super.setBuyerMaker(buyerMaker);
    }
}
