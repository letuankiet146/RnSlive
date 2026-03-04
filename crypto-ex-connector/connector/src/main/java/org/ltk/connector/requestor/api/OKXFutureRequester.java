package org.ltk.connector.requestor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OKXFutureRequester extends BaseRequester {

    @Autowired
    @Qualifier("okxWebClient")
    private WebClient okxWebClient;

    @PostConstruct
    @Override
    protected void init() {
        webClient = okxWebClient;
    }

    @Override
    protected void validateParams(String path, String body) {
        // Validate the path pattern
        String pathPattern = "^/api/v5/.*$"; // Example pattern
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
