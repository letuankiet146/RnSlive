package org.ltk.utils;

import org.ltk.model.exchange.order.SideOrder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TradeHelper {
    public static void delay(long value, TimeUnit unit) {
        long millisecond = TimeUnit.MILLISECONDS.convert(value, unit);
        long start = new Date().getTime();
        long current = new Date().getTime();
        boolean isExit = false;

        while (!isExit) {
            isExit = start + millisecond <= current;
            current = new Date().getTime();
        }
    }

    public static SideOrder opposite(SideOrder side) {
        if (side == SideOrder.BUY)
            return SideOrder.SELL;
        return SideOrder.BUY;
    }

    public static double calcTPPrice(int leverage, String positionSide, double entryPrice, double tpTargetPercent) {
        if (positionSide.equals("LONG")) {
            return entryPrice + ((tpTargetPercent / 100 / leverage * entryPrice));
        }
        if (positionSide.equals("SHORT")) {
            return entryPrice - ((tpTargetPercent / 100 / leverage * entryPrice));
        }
        throw new RuntimeException("Invalid position side: " + positionSide);
    }

    public static double calcPrice(String positionSide, double entryPrice, double amt, double targetNetPnl, double fees) {
        if (positionSide.equals("LONG")) {
            return (targetNetPnl/amt +(1+fees)*entryPrice)/(1-fees);
        }
        if (positionSide.equals("SHORT")) {
            return ((entryPrice*(1-fees))-targetNetPnl/amt)/(1+fees);
        }
        throw new RuntimeException("Invalid position side: " + positionSide);
    }
}
