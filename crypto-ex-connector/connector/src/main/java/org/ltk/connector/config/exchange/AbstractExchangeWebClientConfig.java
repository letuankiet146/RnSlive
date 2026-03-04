package org.ltk.connector.config.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public abstract class AbstractExchangeWebClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExchangeWebClientConfig.class);
    public abstract void config(WebClient.Builder builder);
    public abstract void throwExchangeClientExceptionIfAny(String body);

    protected ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request: \n{} {}\n", clientRequest.method(), clientRequest.url());
            }
            return Mono.just(clientRequest);
        });
    }

    protected ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Response: \t{}", body);
                        }
                        throwExchangeClientExceptionIfAny(body);
                        return Mono.just(ClientResponse.create(clientResponse.statusCode())
                                .headers(headers -> headers.addAll(clientResponse.headers().asHttpHeaders()))
                                .body(body)
                                .build());
                    });
        });
    }
}
