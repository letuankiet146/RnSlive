package org.tk.spot.connector.symbol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.tk.spot.connector.client.ExchangeName;
import org.tk.spot.connector.client.SpotApiUrls;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BinanceSpotExchangeInfoClient implements SpotExchangeInfoClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceSpotExchangeInfoClient.class);
    private static final List<String> QUOTE_PRIORITY = List.of("USDT", "USDC", "USD", "BTC", "ETH");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public BinanceSpotExchangeInfoClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(SpotApiUrls.BINANCE_SPOT_REST).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ExchangeName exchange() {
        return ExchangeName.BINANCE;
    }

    @Override
    public Mono<Map<String, String>> fetchBaseToSymbol() {
        return webClient.get()
                .uri("/api/v3/exchangeInfo")
                .accept(MediaType.APPLICATION_JSON)
                .header("User-Agent", "crypto-spot-ex-connector")
                .header("Accept-Encoding", "identity")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseBaseToSymbolText);
    }

    private Map<String, String> parseBaseToSymbolText(String body) {
        try {
            return parseBaseToSymbol(objectMapper.readTree(body));
        } catch (IOException e) {
            log.warn("Failed to parse Binance exchangeInfo JSON (len={}): {}", body == null ? 0 : body.length(), e.toString());
            return Map.of();
        }
    }

    private Map<String, String> parseBaseToSymbol(JsonNode root) {
        Map<String, Candidate> best = new LinkedHashMap<>();
        JsonNode symbols = root.path("symbols");
        if (!symbols.isArray()) return Map.of();

        for (JsonNode s : symbols) {
            if (!"TRADING".equalsIgnoreCase(s.path("status").asText())) continue;
            String base = s.path("baseAsset").asText(null);
            String quote = s.path("quoteAsset").asText(null);
            String symbol = s.path("symbol").asText(null);
            if (base == null || quote == null || symbol == null) continue;

            String baseU = base.toUpperCase();
            int score = quoteScore(quote.toUpperCase());
            Candidate prev = best.get(baseU);
            if (prev == null || score < prev.score) {
                best.put(baseU, new Candidate(symbol, score));
            }
        }

        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Candidate> e : best.entrySet()) {
            out.put(e.getKey(), e.getValue().symbol);
        }
        return out;
    }

    private int quoteScore(String quoteU) {
        int idx = QUOTE_PRIORITY.indexOf(quoteU);
        return idx >= 0 ? idx : 10_000;
    }

    private record Candidate(String symbol, int score) {}
}

