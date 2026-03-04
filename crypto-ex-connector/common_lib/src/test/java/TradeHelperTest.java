import org.junit.jupiter.api.Test;
import org.ltk.utils.TradeHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TradeHelperTest {
    @Test
    public void testCalcPrice_LONG() {
        double entryPrice = 100000;
        double amt = 0.5;
        double targetNetPnl = 449.75;
        double fees = 0.0005;
        double result = TradeHelper.calcPrice("LONG", entryPrice, amt, targetNetPnl, fees);
        System.out.println(result);
        assertEquals(101000, result, 12);
    }

    @Test
    public void testCalcPrice_SHORT() {
        double entryPrice = 100000;
        double amt = 0.5;
        double targetNetPnl = 450.25;
        double fees = 0.0005;
        double result = TradeHelper.calcPrice("SHORT", entryPrice, amt, targetNetPnl, fees);
        System.out.println(result);
        assertEquals(99000, result, 12);
    }
}
