package org.ltk.connector.requestor.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ltk.connector.client.RESTApiUrl;
import org.ltk.connector.exception.ExchangeClientException;
import org.ltk.connector.utils.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.LinkedHashMap;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes= OKXFutureRequesterTest.class)
@ComponentScan(basePackages = "org.ltk.connector")
public class OKXFutureRequesterTest {
    @Autowired
    private OKXFutureRequester requester;

    @Test
    public void testGetRequestExecute() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("instId", "BTC-USDT-SWAP");
        params.put("sz", 400);
        String url = UrlBuilder.joinQueryParameters(RESTApiUrl.OKX_GET_DEPTH_FULL_URL +"?", params);
        String response = requester.sendRequest(HttpMethod.GET, url);
        System.out.println(response);
        Assertions.assertNotNull(response);
    }

    @Test
    public void testExpectedException() {
        Assertions.assertThrows(ExchangeClientException.class, () -> {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("instId", "abc");
            params.put("sz", 400);
            String url = UrlBuilder.joinQueryParameters(RESTApiUrl.OKX_GET_DEPTH_FULL_URL +"?", params);
            String response = requester.sendRequest(HttpMethod.GET, url);
            System.out.println(response);
        });
    }
}
