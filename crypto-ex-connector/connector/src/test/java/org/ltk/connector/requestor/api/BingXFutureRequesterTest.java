package org.ltk.connector.requestor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ltk.connector.exception.ExchangeClientException;
import org.ltk.connector.utils.ConnectorConst;
import org.ltk.connector.utils.HashAlgorithm;
import org.ltk.connector.utils.SecurityHelper;
import org.ltk.connector.utils.UrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes=BingXFutureRequesterTest.class)
@ComponentScan(basePackages = "org.ltk.connector")
public class BingXFutureRequesterTest {
    public static final String SECRET_KEY = System.getenv("SECRET_KEY");
    private static final String API_KEY = System.getenv("API_KEY");
    @Autowired
    private BingXFutureRequester requester;

    @Test
    public void testGetRequestExecute() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", "BTC-USDT");
        params.put("recvWindow", ConnectorConst.DEFAULT_RECV_WINDOW);
        params.put("timestamp", System.currentTimeMillis());
        String url = UrlBuilder.joinQueryParameters("/openApi/swap/v2/quote/contracts?", params);
        System.out.println(url);
        String response = requester.sendRequest(HttpMethod.GET, url);
        System.out.println(response);
        Assertions.assertNotNull(response);
    }

    @Test
    public void testGetSignedRequestExecute() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("recvWindow", ConnectorConst.DEFAULT_RECV_WINDOW);
        params.put("symbol", "BTC-USDT");
        params.put("timestamp", System.currentTimeMillis());

        String queryParam = UrlBuilder.joinQueryParameters("", params);
        String signed = SecurityHelper.hmac(HashAlgorithm.HMAC_SHA256, queryParam, SECRET_KEY);

        params.put("signature", signed);
        String url = UrlBuilder.joinQueryParameters("/openApi/swap/v3/user/balance?", params);
        System.out.println(url);
        String response = requester.sendRequest(HttpMethod.GET, url, API_KEY);
        System.out.println(response);
        Assertions.assertNotNull(response);
    }

    @Test
    public void testPostRequestExecute() throws JsonProcessingException {
        Map<String, Object> params = new TreeMap<>();
        params.put("recvWindow", ConnectorConst.DEFAULT_RECV_WINDOW);
        params.put("symbol", "BTC-USDT");
        params.put("type", "LIMIT");
        params.put("side", "BUY");
        params.put("positionSide", "LONG");
        params.put("timestamp", System.currentTimeMillis());

        String queryParam = UrlBuilder.joinQueryParameters("", params);
        String signed = SecurityHelper.hmac(HashAlgorithm.HMAC_SHA256, queryParam, SECRET_KEY);
        params.put("signature", signed);

        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(params);
        System.out.println(body);

        String response = requester.sendRequest(HttpMethod.POST, "/openApi/swap/v2/trade/order/test", API_KEY, body);
        System.out.println(response);
        Assertions.assertNotNull(response);
    }

    @Test
    public void testExpectedException() {
        Assertions.assertThrows(ExchangeClientException.class, () -> {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", "abc");
            params.put("recvWindow", ConnectorConst.DEFAULT_RECV_WINDOW);
            params.put("timestamp", System.currentTimeMillis());
            String url = UrlBuilder.joinQueryParameters("/openApi/swap/v2/quote/contracts?", params);
            System.out.println(url);
            String response = requester.sendRequest(HttpMethod.GET, url + "invalid");
            System.out.println(response);
        });
    }
}
