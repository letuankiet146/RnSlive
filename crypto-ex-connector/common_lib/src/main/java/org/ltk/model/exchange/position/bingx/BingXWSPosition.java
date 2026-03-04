package org.ltk.model.exchange.position.bingx;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ltk.model.exchange.position.Position;

public class BingXWSPosition extends Position {

    @JsonProperty("s")
    @Override
    public String getSymbol() {
        return super.getSymbol();
    }

    @JsonProperty("s")
    @Override
    public void setSymbol(String symbol) {
        super.setSymbol(symbol);
    }

    @JsonProperty("ps")
    @Override
    public String getPositionSide() {
        return super.getPositionSide();
    }

    @JsonProperty("ps")
    @Override
    public void setPositionSide(String positionSide) {
        super.setPositionSide(positionSide);
    }

    @JsonProperty("pa")
    @Override
    public String getPositionAmt() {
        return super.getPositionAmt();
    }

    @JsonProperty("pa")
    @Override
    public void setPositionAmt(String positionAmt) {
        super.setPositionAmt(positionAmt);
    }

    @JsonProperty("ep")
    @Override
    public String getAvgPrice() {
        return super.getAvgPrice();
    }

    @JsonProperty("ep")
    @Override
    public void setAvgPrice(String avgPrice) {
        super.setAvgPrice(avgPrice);
    }

    @JsonProperty("up")
    @Override
    public String getUnrealizedProfit() {
        return super.getUnrealizedProfit();
    }

    @JsonProperty("up")
    @Override
    public void setUnrealizedProfit(String unrealizedProfit) {
        super.setUnrealizedProfit(unrealizedProfit);
    }

    @JsonProperty("cr")
    @Override
    public String getRealizedProfit() {
        return super.getRealizedProfit();
    }

    @JsonProperty("cr")
    @Override
    public void setRealizedProfit(String realizedProfit) {
        if(Double.parseDouble(getUnrealizedProfit()) == 0) {
            super.setRealizedProfit(realizedProfit);
        } else {
            super.setRealizedProfit("0");
        }
    }
}
