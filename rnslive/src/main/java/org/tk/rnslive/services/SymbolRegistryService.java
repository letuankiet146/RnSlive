package org.tk.rnslive.services;

import org.ltk.connector.client.ExchangeName;
import org.ltk.connector.service.ExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tk.rnslive.model.symbol.MultiExchangeSymbolRegistry;
import org.tk.rnslive.model.symbol.SymbolType;
import org.tk.rnslive.services.symbol.ExchangeInfoParser;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * Builds and holds {@link MultiExchangeSymbolRegistry} per {@link SymbolType} (FUTURE, SPOT)
 * by calling the connector's ExchangeInfo API for each exchange and parsing with the appropriate parser.
 * <p>
 * Designed to scale to many exchanges (e.g. 5+): add a new {@link ExchangeInfoParser} implementation
 * and ensure the connector's {@link ExchangeService#getExchangeInfo(ExchangeName)} supports that exchange.
 * No code changes are required elsewhere.
 * <p>
 * Example: getSymbol(ExchangeName.OKX, "BTC") → "BTC-USDT-SWAP", getSymbol(ExchangeName.BINANCE, "BTC") → "BTCUSDT".
 */
@Service
public class SymbolRegistryService {

    private static final Logger LOG = LoggerFactory.getLogger(SymbolRegistryService.class);

    private final ExchangeService exchangeService;
    private final List<ExchangeInfoParser> parsers;

    /** Registry per symbol type; default usage is FUTURE. */
    private final Map<SymbolType, MultiExchangeSymbolRegistry> registries = new EnumMap<>(SymbolType.class);

    public SymbolRegistryService(ExchangeService exchangeService, List<ExchangeInfoParser> parsers) {
        this.exchangeService = exchangeService;
        this.parsers = parsers;
    }

    /**
     * Returns the set of exchanges that currently have symbol data for the given type.
     * Use this for defaults (e.g. "common bases across all loaded exchanges") instead of hardcoding names.
     */
    public Set<ExchangeName> getLoadedExchanges(SymbolType symbolType) {
        return getRegistry(symbolType).getExchanges();
    }

    @PostConstruct
    public void loadRegistries() {
        for (SymbolType type : SymbolType.values()) {
            loadRegistry(type).block();
        }
    }

    /**
     * (Re)load registry for the given symbol type from all configured exchanges.
     */
    public Mono<MultiExchangeSymbolRegistry> loadRegistry(SymbolType symbolType) {
        return Mono.defer(() -> {
            Map<ExchangeName, Map<String, String>> exchangeToBaseToSymbol = new java.util.HashMap<>();
            return Mono.when(parsers.stream()
                    .map(parser -> exchangeService.getExchangeInfo(parser.getExchange())
                            .doOnNext(json -> {
                                Map<String, String> baseToSymbol = parser.parse(json, symbolType);
                                if (!baseToSymbol.isEmpty()) {
                                    exchangeToBaseToSymbol.put(parser.getExchange(), baseToSymbol);
                                }
                            })
                            .onErrorResume(e -> {
                                LOG.warn("Failed to load exchange info for {}: {}", parser.getExchange(), e.getMessage());
                                return Mono.empty();
                            }))
                    .toList())
                    .then(Mono.fromCallable(() -> {
                        MultiExchangeSymbolRegistry registry = new MultiExchangeSymbolRegistry(symbolType, exchangeToBaseToSymbol);
                        registries.put(symbolType, registry);
                        LOG.info("Loaded symbol registry for {}: exchanges={}, total bases (intersection)={}",
                                symbolType, registry.getExchanges(), registry.getBasesExistingOnAll(registry.getExchanges().toArray(new ExchangeName[0])).size());
                        return registry;
                    }));
        });
    }

    /**
     * Returns the registry for the given type (FUTURE or SPOT). May be empty if not yet loaded.
     */
    public MultiExchangeSymbolRegistry getRegistry(SymbolType symbolType) {
        return registries.getOrDefault(symbolType, new MultiExchangeSymbolRegistry(symbolType, Map.of()));
    }

    /**
     * Returns the exchange-specific symbol for the base asset (e.g. "BTC").
     * Uses FUTURE registry by default.
     *
     * @param exchange  e.g. ExchangeName.OKX or ExchangeName.BINANCE
     * @param baseAsset e.g. "BTC"
     * @return e.g. "BTC-USDT-SWAP" for OKX, "BTCUSDT" for Binance, or null if not found
     */
    public String getSymbol(ExchangeName exchange, String baseAsset) {
        return getSymbol(exchange, baseAsset, SymbolType.FUTURE);
    }

    /**
     * Returns the exchange-specific symbol for the base asset and symbol type.
     */
    public String getSymbol(ExchangeName exchange, String baseAsset, SymbolType symbolType) {
        return getRegistry(symbolType).getSymbol(exchange, baseAsset);
    }

    /**
     * True if the base asset exists on both given exchanges (using FUTURE registry).
     */
    public boolean existsOnBoth(ExchangeName a, ExchangeName b, String baseAsset) {
        return getRegistry(SymbolType.FUTURE).existsOnBoth(a, b, baseAsset);
    }

    /**
     * Returns the exchange-specific symbol for each of the given exchanges, for the base asset.
     * Entries with null symbol are omitted (base not listed on that exchange).
     */
    public Map<ExchangeName, String> getSymbolsByExchange(ExchangeName[] exchanges, String baseAsset, SymbolType symbolType) {
        Map<ExchangeName, String> result = new HashMap<>();
        MultiExchangeSymbolRegistry registry = getRegistry(symbolType);
        for (ExchangeName ex : exchanges) {
            String symbol = registry.getSymbol(ex, baseAsset);
            if (symbol != null) result.put(ex, symbol);
        }
        return Map.copyOf(result);
    }

    /**
     * True if the base asset exists on all given exchanges.
     */
    public boolean existsOnAll(Set<ExchangeName> exchanges, String baseAsset, SymbolType symbolType) {
        return getRegistry(symbolType).existsOnAll(exchanges, baseAsset);
    }

    /**
     * Base assets that exist on all given exchanges (FUTURE by default).
     */
    public Set<String> getBasesExistingOnAll(SymbolType symbolType, ExchangeName... exchanges) {
        return getRegistry(symbolType).getBasesExistingOnAll(exchanges);
    }
}
