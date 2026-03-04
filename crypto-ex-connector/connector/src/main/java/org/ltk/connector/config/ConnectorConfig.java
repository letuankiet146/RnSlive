package org.ltk.connector.config;

import org.ltk.connector.config.exchange.BinanceWebClientFilter;
import org.ltk.connector.config.exchange.BingXWebClientFilter;
import org.ltk.connector.config.exchange.OKXWebClientFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;


@Configuration
public class ConnectorConfig {
    @Autowired
    private BinanceWebClientFilter binanceWebClientFilter;

    @Autowired
    private BingXWebClientFilter bingxWebClientFilter;

    @Autowired
    private OKXWebClientFilter okxWebClientFilter;

    @Bean("binanceWebClient")
    public WebClient binanceWebClient() {
        return buildWebClient("https://fapi.binance.com", binanceWebClientFilter::config);
    }

    @Bean("bingXWebClient")
    public WebClient bingXWebClient() {
        return buildWebClient("https://open-api.bingx.com", bingxWebClientFilter::config);
    }

    @Bean("okxWebClient")
    public WebClient okxWebClient() {
        return buildWebClient("https://www.okx.com/", okxWebClientFilter::config);
    }

    private WebClient buildWebClient(String baseUrl, Consumer<WebClient.Builder> config) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .apply(config)
                .build();
    }
}
