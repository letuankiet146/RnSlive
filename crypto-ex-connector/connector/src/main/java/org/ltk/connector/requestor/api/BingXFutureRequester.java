package org.ltk.connector.requestor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class BingXFutureRequester extends BaseRequester{

    @Autowired
    @Qualifier("bingXWebClient")
    private WebClient bingXWebClient;

    @PostConstruct
    @Override
    public void init() {
        webClient = bingXWebClient;
    }

    /**
     * @deprecated Use {@link BaseRequester#sendRequest(HttpMethod, String, Map, String)} instead
     * @param method
     * @param path
     * @param apiKey
     * @param body
     * @return
     */
    @Deprecated
    public String sendRequest(HttpMethod method, String path, String apiKey, String body) {
        Map<String, String> headers = Map.of("X-BX-APIKEY", apiKey);
        return sendRequest(method, path, headers, body);
    }

    /**
     * @deprecated Use {@link BaseRequester#sendRequest(HttpMethod, String, Map)} instead
     * @param method
     * @param path
     * @param apiKey
     * @return
     */
    @Deprecated
    public String sendRequest(HttpMethod method, String path, String apiKey) {
        return sendRequest(method, path, apiKey, null);
    }

    @Override
    protected void validateParams(String path, String body) {
        // Validate the path pattern
        String pathPattern = "^/openApi/.*$"; // Example pattern
        if (!path.matches(pathPattern)) {
            throw new IllegalArgumentException("Invalid path pattern");
        }

        // Validate the body is a valid JSON
        if (body != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.readTree(body);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON body", e);
            }
        }
    }
}
