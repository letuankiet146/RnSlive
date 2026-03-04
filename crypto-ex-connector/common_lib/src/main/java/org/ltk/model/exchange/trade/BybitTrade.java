package org.ltk.model.exchange.trade;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ltk.model.AggregateTrade;

import java.math.BigDecimal;

public class BybitTrade extends AggregateTrade {
    @JsonProperty("S")
    private String takerSide;

    @Override
    @JsonProperty("p")
    public void setPrice(BigDecimal price) {
        super.setPrice(price);
    }

    @Override
    @JsonProperty("v")
    public void setQty(double qty) {
        super.setQty(qty);
    }

    public String getTakerSide() {
        return takerSide;
    }

    public void setTakerSide(String takerSide) {
        this.takerSide = takerSide;
        super.setBuyerMaker("Sell".equals(takerSide));
    }
}
