package org.ltk.connector.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ltk.connector.client.ExchangeName;
import org.ltk.utils.TradeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes= ExchangeServiceImpl_MarketData_Test.class)
@ComponentScan(basePackages = "org.ltk.connector")
public class ExchangeServiceImpl_MarketData_Test {

    private static final String SYMBOL = "BTC-USDT";

    @Autowired
    private ExchangeService exchangeServiceImpl;

    @Test
    public void testGetMarkPrice() {
        var markPrice = exchangeServiceImpl.getMarkPrice(ExchangeName.BINGX, SYMBOL);
        Assertions.assertTrue(markPrice > 0);
    }

    @Test
    public void testSubscribeDepth() throws InterruptedException {
        exchangeServiceImpl.subscribeDepth(ExchangeName.BINGX, SYMBOL, "500ms", System.out::println);
    }

    @Test
    public void testSubscribeTradeDetail() throws InterruptedException {
        exchangeServiceImpl.subscribeTradeDetail(ExchangeName.BINGX, SYMBOL, "500ms", System.out::println);
        TradeHelper.delay(2, TimeUnit.SECONDS);
    }

    @Test
    public void testGetDepth() {
        var depth = exchangeServiceImpl.getDepth(ExchangeName.BINANCE, "BTCUSDT", 10);
        Assertions.assertNotNull(depth);
        Assertions.assertEquals(depth.getAsks().size(), 10);

        depth = null;
        depth = exchangeServiceImpl.getDepth(ExchangeName.OKX, "BTC-USDT-SWAP", 50);
        Assertions.assertNotNull(depth);
        Assertions.assertEquals(depth.getAsks().size(), 50);
    }

    @Test
    public void testGetKline() {
        var klineList = exchangeServiceImpl.getKline(ExchangeName.BINANCE,"BTCUSDT", "1m",null, null, null);
        Assertions.assertNotNull(klineList);
        Assertions.assertFalse(klineList.isEmpty());
    }

    @Test
    public void testSubscribeMarkPrice() {
        exchangeServiceImpl.subscribeMarkPrice(ExchangeName.OKX, "BTC-USDT-SWAP", null, System.out::println);
        TradeHelper.delay(2, TimeUnit.SECONDS);
    }

    @Test
    public void testSubscribeForceOrder() {
        exchangeServiceImpl.subscribeForceOrder(ExchangeName.BINANCE, "BTCUSDT", System.out::println);
        TradeHelper.delay(2, TimeUnit.SECONDS);
    }
}
