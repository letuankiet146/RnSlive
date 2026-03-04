package org.ltk.connector.config.exchange;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.exception.ExchangeClientException;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;


public class BinanceWebClientFilter extends AbstractExchangeWebClientConfig {
    private static final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void config(WebClient.Builder builder) {
        builder.filter(logRequest())
                .filter(logResponse())
        ;
    }

    @Override
    public void throwExchangeClientExceptionIfAny(String body) {
        String code;
        try {
            code = mapper.readTree(body).path("code").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if ((StringUtils.hasText(code) && Integer.parseInt(code) != 0) && !body.contains("listenKey")) {
            throw new ExchangeClientException(body);
        }
    }
}
