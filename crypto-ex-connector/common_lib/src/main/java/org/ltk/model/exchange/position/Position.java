package org.ltk.model.exchange.position;

public class Position {
    private String id;
    private String symbol;
    private int leverage;
    private String positionSide;
    private String positionAmt;
    private String avgClosePrice;
    private String avgPrice;
    private String unrealizedProfit;
    private String realizedProfit;
    private String netProfit;
    private String fee;
    private long updateTime;
    private Integer userId=1;
    private String tpId;
    private String slId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    public int getLeverage() {
        return leverage;
    }

    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }
    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public String getPositionAmt() {
        return positionAmt;
    }

    public void setPositionAmt(String positionAmt) {
        this.positionAmt = positionAmt;
    }

    public String getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(String avgPrice) {
        this.avgPrice = avgPrice;
    }
    public String getAvgClosePrice() {
        return avgClosePrice;
    }

    public void setAvgClosePrice(String avgClosePrice) {
        this.avgClosePrice = avgClosePrice;
    }
    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public void setUnrealizedProfit(String unrealizedProfit) {
        this.unrealizedProfit = unrealizedProfit;
    }

    public String getRealizedProfit() {
        return realizedProfit;
    }

    public void setRealizedProfit(String realizedProfit) {
        this.realizedProfit = realizedProfit;
    }

    public String getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(String netProfit) {
        this.netProfit = netProfit;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    public String getTpId() {
        return tpId;
    }

    public void setTpId(String tpId) {
        this.tpId = tpId;
    }

    public String getSlId() {
        return slId;
    }

    public void setSlId(String slId) {
        this.slId = slId;
    }

    @Override
    public String toString() {
        return "Position{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", positionSide='" + positionSide + '\'' +
                ", positionAmt='" + positionAmt + '\'' +
                ", avgClosePrice='" + avgClosePrice + '\'' +
                ", avgPrice='" + avgPrice + '\'' +
                ", netProfit='" + netProfit + '\'' +
                ", realizedProfit='" + realizedProfit + '\'' +
                '}';
    }
}
