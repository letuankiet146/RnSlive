package org.tk.spot.connector.symbol;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tk.spot.connector.client.ExchangeName;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class SpotSymbolRegistryService {

    private static final Logger log = LoggerFactory.getLogger(SpotSymbolRegistryService.class);

    private final Map<ExchangeName, SpotExchangeInfoClient> clients;
    private final AtomicReference<SpotSymbolRegistry> registryRef = new AtomicReference<>(
            new SpotSymbolRegistry(Map.of())
    );

    public SpotSymbolRegistryService(Set<SpotExchangeInfoClient> clients) {
        Map<ExchangeName, SpotExchangeInfoClient> map = new EnumMap<>(ExchangeName.class);
        for (SpotExchangeInfoClient c : clients) {
            if (c != null && c.exchange() != null) {
                map.put(c.exchange(), c);
            }
        }
        this.clients = map;
    }

    @PostConstruct
    public void warmup() {
        // Fire-and-forget cache warmup; endpoint /spot/symbol/refresh can be called for explicit reload.
        loadRegistry()
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    public Set<ExchangeName> getLoadedExchanges() {
        return registryRef.get().getExchanges();
    }

    public SpotSymbolRegistry getRegistry() {
        return registryRef.get();
    }

    public String getSymbol(ExchangeName exchange, String base) {
        if (exchange == null || base == null) return null;
        return registryRef.get().getSymbol(exchange, base.strip().toUpperCase());
    }

    public boolean existsOnAll(Set<ExchangeName> exchanges, String base) {
        if (base == null) return false;
        String baseU = base.strip().toUpperCase();
        return registryRef.get().existsOnAll(exchanges, baseU);
    }

    public Map<ExchangeName, String> getSymbolsByExchange(ExchangeName[] exchanges, String base) {
        String baseU = base == null ? "" : base.strip().toUpperCase();
        SpotSymbolRegistry reg = registryRef.get();
        return Arrays.stream(exchanges)
                .distinct()
                .collect(Collectors.toMap(
                        ex -> ex,
                        ex -> {
                            String s = reg.getSymbol(ex, baseU);
                            return s == null ? "" : s;
                        },
                        (a, b) -> a,
                        () -> new EnumMap<>(ExchangeName.class)
                ));
    }

    public Set<String> getBasesExistingOnAll(ExchangeName[] exchanges) {
        SpotSymbolRegistry reg = registryRef.get();
        if (exchanges == null || exchanges.length == 0) return Set.of();

        Set<String> out = null;
        for (ExchangeName ex : exchanges) {
            Map<String, String> m = reg.baseToSymbolByExchange().get(ex);
            Set<String> bases = m == null ? Set.of() : m.keySet();
            if (out == null) out = new LinkedHashSet<>(bases);
            else out.retainAll(bases);
        }
        return out == null ? Set.of() : out;
    }

    public Mono<SpotSymbolRegistry> loadRegistry() {
        // Only Binance and OKX are considered "real" right now; others are placeholders.
        return Mono.zip(
                        fetchFor(ExchangeName.BINANCE),
                        fetchFor(ExchangeName.OKX)
                )
                .map(tuple -> {
                    Map<ExchangeName, Map<String, String>> m = new EnumMap<>(ExchangeName.class);
                    m.put(ExchangeName.BINANCE, tuple.getT1());
                    m.put(ExchangeName.OKX, tuple.getT2());
                    SpotSymbolRegistry reg = new SpotSymbolRegistry(m);
                    registryRef.set(reg);
                    log.info("Loaded spot symbols: BINANCE={}, OKX={}", tuple.getT1().size(), tuple.getT2().size());
                    return reg;
                });
    }

    private Mono<Map<String, String>> fetchFor(ExchangeName exchange) {
        SpotExchangeInfoClient c = clients.get(exchange);
        if (c == null) {
            return new PlaceholderSpotExchangeInfoClient(exchange).fetchBaseToSymbol();
        }
        return c.fetchBaseToSymbol()
                .doOnError(e -> log.warn("Failed loading symbols for {}: {}", exchange, e.toString()))
                .onErrorReturn(Map.of());
    }
}

