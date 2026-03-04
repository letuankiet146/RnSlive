package org.ltk.model;

import java.util.Optional;

public class TradingConstant {
    public static final boolean IS_TRADE = Optional.ofNullable(System.getenv("IS_TRADE")).map(Boolean::parseBoolean).orElse(false);
    public static final String SYMBOL = System.getenv("SYMBOL");
    public static final int LEVERAGE = Optional.ofNullable(System.getenv("LEVERAGE")).map(Integer::parseInt).orElse(0);
    public static final double QUANTITY = Optional.ofNullable(System.getenv("QUANTITY")).map(Double::parseDouble).orElse(0.0);
    public static final Double TP_TARGET_PERCENT = Optional.ofNullable(System.getenv("TP_TARGET_PERCENT")).map(Double::parseDouble).orElse(null);
    public static final Double SL_TARGET_PERCENT = Optional.ofNullable(System.getenv("SL_TARGET_PERCENT")).map(Double::parseDouble).orElse(null);
    public static final int T_1M = Optional.ofNullable(System.getenv("T_1M")).map(Integer::parseInt).orElse(1000000);
    public static final int T_100k = Optional.ofNullable(System.getenv("T_100k")).map(Integer::parseInt).orElse(100000);
    public static final double MAKER_FEES = 0.0002;
    public static final double TAKER_FEES = 0.0005;

    public static final String LAST_TRADE_SIDE = "lastTradeSide";
}
