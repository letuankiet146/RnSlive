package org.tk.spot.connector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Use the JDK HTTP client connector to avoid Reactor Netty TLS issues in some environments.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                // Binance exchangeInfo can be very large; keep this high enough for symbol discovery.
                .codecs(c -> c.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }
}

