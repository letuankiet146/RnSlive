package org.ltk.connector.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.exception.ExchangeClientException;
import org.ltk.model.exchange.order.Order;
import org.ltk.model.exchange.order.OrderType;
import org.ltk.model.exchange.order.SideOrder;
import org.ltk.model.exchange.order.bingx.BingXOrder;
import org.ltk.model.exchange.position.Position;
import org.ltk.utils.TradeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes= ExchangeServiceImplTest.class)
@ComponentScan(basePackages = "org.ltk.connector")
public class ExchangeServiceImplTest {

    private static final String SYMBOL = "BTC-USDT";
    private static final SideOrder SIDE = SideOrder.BUY;
    private static final double QTY = 0.0001;

    private static double price;

    @Autowired
    private ExchangeService exchangeServiceImpl;

    @BeforeEach
    public void setUp() {
//        var markPrice = exchangeServiceImpl.getMarkPrice(ExchangeName.BINGX, SYMBOL);
//        price = markPrice * 0.5f;
    }

    @AfterEach
    public void tearDown() {
//        exchangeServiceImpl.cancelAllOpenOrders(ExchangeName.BINGX, SYMBOL);
    }

    @Test
    public void testGetMarkPrice() {
        var markPrice = exchangeServiceImpl.getMarkPrice(ExchangeName.BINGX, SYMBOL);
        Assertions.assertTrue(markPrice > 0);
    }

    @Test
    public void testCreateLimitOrder() {
        Order order = exchangeServiceImpl.createLimitOrder(ExchangeName.BINGX, SYMBOL, SIDE, price, QTY);
        Assertions.assertNotNull(order);
        Assertions.assertEquals(SYMBOL, order.getSymbol());
        Assertions.assertEquals(SIDE, order.getSide());
        Assertions.assertEquals(price, order.getPrice(), 2);
        Assertions.assertEquals(QTY, order.getQuantity(), 3);
    }

    @Test
    public void testCreateMarketOrder() {
        Order order = exchangeServiceImpl.createMarketOrder(ExchangeName.BINGX, SYMBOL, SIDE, QTY);
        Assertions.assertNotNull(order);
        Assertions.assertEquals(SYMBOL, order.getSymbol());
        Assertions.assertEquals(SIDE, order.getSide());
        Assertions.assertEquals(QTY, order.getQuantity(), 3);
    }

    @Test
    public void testGetOpenOrders() {
        var orders = exchangeServiceImpl.getOpenOrders(ExchangeName.BINGX, SYMBOL);
        Assertions.assertNotNull(orders);
    }

    @Test
    public void testCancelAllOpenOrders() {
        exchangeServiceImpl.cancelAllOpenOrders(ExchangeName.BINGX, SYMBOL);
    }

    @Test
    public void testGetPosition() {
        List<Position> position = exchangeServiceImpl.getPosition(ExchangeName.BINGX, SYMBOL);
        Assertions.assertTrue(position.isEmpty());
    }

    @Test
    public void testGetPositionHistory() {
        List<Position> positions = exchangeServiceImpl.getPositionHistory(ExchangeName.BINGX, SYMBOL,  "123456");
        Assertions.assertTrue(positions.isEmpty());
        positions = exchangeServiceImpl.getPositionHistory(ExchangeName.BINGX, SYMBOL,  "1851398604104888320");
        System.out.println(positions);
        Assertions.assertFalse(positions.isEmpty());
    }

    @Test
    public void testCreateTakeProfitOrder() {
        ExchangeClientException exception = Assertions.assertThrows(ExchangeClientException.class, () -> {
            exchangeServiceImpl.createTakeProfitOrder(ExchangeName.BINGX, SYMBOL, SideOrder.BUY, 50000, 0.001);
        });
        Assertions.assertTrue(exception.getMessage().contains("position not exist"));
    }

    @Test
    public void testCreateStopLossOrder() {
        ExchangeClientException exception = Assertions.assertThrows(ExchangeClientException.class, () -> {
            exchangeServiceImpl.createStopLossOrder(ExchangeName.BINGX, SYMBOL, SideOrder.BUY, 66000, 0.001);
        });
        Assertions.assertTrue(exception.getMessage().contains("position not exist"));
    }

    @Test
    public void testCloseAllPosition() {
        exchangeServiceImpl.closeAllPosition(ExchangeName.BINGX, SYMBOL);
    }

    @Test
    public void testSubscribeDepth() throws InterruptedException {
        exchangeServiceImpl.subscribeDepth(ExchangeName.BINGX, SYMBOL, "500ms", System.out::println);
    }

    @Test
    public void testSubscribeTradeDetail() throws InterruptedException {
        exchangeServiceImpl.subscribeTradeDetail(ExchangeName.BINGX, SYMBOL, "500ms", System.out::println);
        Thread.sleep(5000);
    }

    @Test
    public void testCancelOrder() {
        Order order = exchangeServiceImpl.createLimitOrder(ExchangeName.BINGX, SYMBOL, SIDE, price, QTY);
        exchangeServiceImpl.cancelOrder(ExchangeName.BINGX, SYMBOL, order.getOrderId());
        ExchangeClientException exception = Assertions.assertThrows(ExchangeClientException.class, () -> {
            exchangeServiceImpl.cancelOrder(ExchangeName.BINGX, SYMBOL, "123456");
        });
        Assertions.assertTrue(exception.getMessage().contains("order not exist"));
    }

    @Test
    public void testCancelMultiOrder() {
        Order order1 = exchangeServiceImpl.createLimitOrder(ExchangeName.BINGX, SYMBOL, SIDE, price, QTY);
        Order order2 = exchangeServiceImpl.createLimitOrder(ExchangeName.BINGX, SYMBOL, SIDE, price*0.9, QTY);
        Set<Long>  ids = Set.of(Long.valueOf(order1.getOrderId()), Long.valueOf(order2.getOrderId()));
        exchangeServiceImpl.cancelMultiOrder(ExchangeName.BINGX, SYMBOL, ids);
    }

    @Test
    public void testCreateMultiOrders() {
        BingXOrder order1 = new BingXOrder();
        order1.setType(OrderType.LIMIT);
        order1.setSymbol(SYMBOL);
        order1.setSide(SideOrder.BUY);
        order1.setPositionSide("LONG");
        order1.setPrice(price);
        order1.setQuantity(QTY);

        BingXOrder order2 = new BingXOrder();
        order2.setType(OrderType.LIMIT);
        order2.setSymbol(SYMBOL);
        order2.setSide(SideOrder.SELL);
        order2.setPositionSide("SHORT");
        order2.setPrice(price*4);
        order2.setQuantity(QTY);

        Set<Order> orders = Set.of(order1, order2);

        List<Order> result = exchangeServiceImpl.createMultiOrders(ExchangeName.BINGX, SYMBOL, orders);
        Assertions.assertNotNull(result);
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
    public void testSubscribeMarkPrice() {
        exchangeServiceImpl.subscribeMarkPrice(ExchangeName.OKX, "BTC-USDT-SWAP", null, System.out::println);
        TradeHelper.delay(10, TimeUnit.SECONDS);
    }

    @Test
    public void testSubscribeForceOrder() {
        exchangeServiceImpl.subscribeForceOrder(ExchangeName.BINANCE, "BTCUSDT", System.out::println);
        TradeHelper.delay(10, TimeUnit.SECONDS);
    }
}
