package org.ltk.connector.requestor.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes= BingXFutureWebSocketTest.class)
@ComponentScan(basePackages = "org.ltk.connector")
public class BingXFutureWebSocketTest {
    private static final String API_KEY = System.getenv("API_KEY");

    @Autowired
    private BingXFutureWebSocket webSocket;

    @Test
    public void testSubscribe() {
        webSocket.subscribeAccountData(API_KEY, null, System.out::println);
    }

}
