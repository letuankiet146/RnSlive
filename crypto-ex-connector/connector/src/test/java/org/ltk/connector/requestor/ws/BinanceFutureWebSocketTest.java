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
public class BinanceFutureWebSocketTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinanceFutureWebSocketTest.class);
    @Autowired
    private BinanceFutureWebSocket webSocket;

    @Test
    public void testSubscribe(){
        String path = "bnbusdt@aggTrade";
        webSocket.subscribe(path, LOGGER::info);
    }
}
