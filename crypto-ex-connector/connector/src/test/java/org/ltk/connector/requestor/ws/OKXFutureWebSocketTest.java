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
public class OKXFutureWebSocketTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OKXFutureWebSocketTest.class);
    @Autowired
    private OKXFutureWebSocket okxWS;

    @Test
    public void testSubscribe() {
        String subscribeMsg = "{\n" +
                "    \"op\":\"subscribe\",\n" +
                "    \"args\":[\n" +
                "        {\n" +
                "            \"channel\":\"mark-price\",\n" +
                "            \"instId\":\"BTC-USDT-SWAP\"\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        okxWS.subscribe(subscribeMsg, LOGGER::info);
    }
}
