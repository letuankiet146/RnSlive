package org.ltk.connector.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.config.exchange.BinanceWebClientFilter;
import org.ltk.connector.config.exchange.BingXWebClientFilter;
import org.ltk.connector.config.exchange.OKXWebClientFilter;
import org.ltk.model.AccountKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Timer;

@Configuration
public class ApplicationConfig {
    @Bean
    public AccountKey accountKey() {
        AccountKey accountKey = new AccountKey();
        accountKey.setApiKey(System.getenv("API_KEY"));
        accountKey.setSecretKey(System.getenv("SECRET_KEY"));
        accountKey.setEmail(System.getenv("EMAIL"));
        accountKey.setChatId(System.getenv("CHAT_ID"));
        return accountKey;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Bean
    Timer timer() {
        return new Timer();
    }

    @Bean
    public BingXWebClientFilter bingxWebClientConfig() {
        return new BingXWebClientFilter();
    }

    @Bean
    public BinanceWebClientFilter binanceWebClientConfig() {
        return new BinanceWebClientFilter();
    }

    @Bean
    public OKXWebClientFilter okxWebClientConfig() {
        return new OKXWebClientFilter();
    }
}
