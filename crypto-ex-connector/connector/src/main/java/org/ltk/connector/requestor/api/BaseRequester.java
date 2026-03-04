package org.ltk.connector.requestor.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public abstract class BaseRequester {
    protected WebClient webClient;
    protected abstract void init();
    protected abstract void validateParams(String path, String body);

    private String baseSendRequest(HttpMethod method, String path, Consumer<HttpHeaders> headersConsumer, String body) {
        validateParams(path, body);
        var spec = webClient
                .method(method)
                .uri(path);
        if (headersConsumer != null) {
            spec.headers(headersConsumer);
        }
        if (body != null) {
            spec.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            spec.bodyValue(body);
        }
        return spec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    if (e instanceof TimeoutException) {
                        String message = String.format("Timeout exception while sending request %s %s", method, path);
                        throw new RuntimeException(message,e);
                    }
                    throw new RuntimeException(e);
                })
                .block();
    }

    public String sendRequest(HttpMethod method, String path, Map<String, String> headers, String body) {
        Consumer<HttpHeaders> headersConsumer = headers == null ? null : httpHeaders -> headers.forEach(httpHeaders::add);
        return baseSendRequest(method, path, headersConsumer, body);
    }

    public String sendRequest(HttpMethod method, String path, Map<String, String> headers) {
        Consumer<HttpHeaders> headersConsumer = headers == null ? null : httpHeaders -> headers.forEach(httpHeaders::add);
        return baseSendRequest(method, path, headersConsumer, null);
    }
    public String sendRequest(HttpMethod method, String path) {
        return baseSendRequest(method, path, null, null);
    }
}
