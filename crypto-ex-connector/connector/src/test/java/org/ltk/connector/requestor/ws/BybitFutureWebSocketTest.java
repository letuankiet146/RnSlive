package org.ltk.connector.requestor.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes= BingXFutureWebSocketTest.class)
@ComponentScan(basePackages = "org.ltk.connector")
public class BybitFutureWebSocketTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BybitFutureWebSocketTest.class);
    @Autowired
    private BybitFutureWebSocket bybitWS;

    @Test
    public void testSubscribe() {
        String subscribeMsg = String.format("{\"req_id\": \"test\",\"op\": \"subscribe\",\"args\": [\"%s\"]}", "publicTrade.BTCUSDT");
        bybitWS.subscribe(subscribeMsg, LOGGER::info);
    }

    @Test
    public void testSubscribeOrderbook() {
        String subscribeMsg = String.format("{\"req_id\": \"test\",\"op\": \"subscribe\",\"args\": [\"%s\"]}", "orderbook.500.BTCUSDT");
        bybitWS.subscribe(subscribeMsg, LOGGER::info);
    }
}
