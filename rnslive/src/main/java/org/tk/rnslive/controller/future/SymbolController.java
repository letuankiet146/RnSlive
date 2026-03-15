package org.tk.rnslive.controller.future;

import org.ltk.connector.client.ExchangeName;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.rnslive.model.symbol.SymbolType;
import org.tk.rnslive.services.SymbolRegistryService;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST API for multi-exchange symbol lookup.
 * Symbols are loaded from connector ExchangeInfo and filtered by type (FUTURE or SPOT).
 * Supports any number of exchanges; add new parsers and connector support to extend.
 */
@RestController
@RequestMapping("/future")
public class SymbolController {

    private final SymbolRegistryService symbolRegistryService;

    public SymbolController(SymbolRegistryService symbolRegistryService) {
        this.symbolRegistryService = symbolRegistryService;
    }

    /**
     * List exchanges that have symbol data loaded for the given type.
     * Example: GET /future/symbol/exchanges  or  /future/symbol/exchanges?type=SPOT
     */
    @GetMapping(value = "/symbol/exchanges", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getLoadedExchanges(
            @RequestParam(required = false, defaultValue = "FUTURE") SymbolType type
    ) {
        Set<ExchangeName> exchanges = symbolRegistryService.getLoadedExchanges(type);
        return Map.of(
                "type", type.name(),
                "exchanges", exchanges.stream().map(ExchangeName::name).sorted().collect(Collectors.toList())
        );
    }

    /**
     * Get exchange-specific symbol for a base asset.
     * Example: GET /future/symbol?exchange=OKX&base=BTC → "BTC-USDT-SWAP"
     *          GET /future/symbol?exchange=BINANCE&base=BTC → "BTCUSDT"
     */
    @GetMapping(value = "/symbol", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getSymbol(
            @RequestParam ExchangeName exchange,
            @RequestParam String base,
            @RequestParam(required = false, defaultValue = "FUTURE") SymbolType type
    ) {
        String symbol = symbolRegistryService.getSymbol(exchange, base, type);
        return Map.of("exchange", exchange.name(), "base", base.toUpperCase(), "symbol", symbol != null ? symbol : "");
    }

    /**
     * Check if a base exists on both given exchanges.
     * Example: GET /future/symbol/exists-on-both?base=BTC&a=BINANCE&b=OKX
     */
    @GetMapping(value = "/symbol/exists-on-both", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> existsOnBoth(
            @RequestParam String base,
            @RequestParam ExchangeName a,
            @RequestParam ExchangeName b,
            @RequestParam(required = false, defaultValue = "FUTURE") SymbolType type
    ) {
        boolean exists = symbolRegistryService.getRegistry(type).existsOnBoth(a, b, base);
        return Map.of(
                "base", base.toUpperCase(),
                "existsOnBoth", exists,
                "symbols", symbolRegistryService.getSymbolsByExchange(new ExchangeName[]{a, b}, base, type)
        );
    }

    /**
     * Check if a base exists on all given exchanges and return symbol per exchange.
     * When no exchanges param is provided, uses all currently loaded exchanges.
     * Example: GET /future/symbol/exists-on-all?base=BTC  or  /future/symbol/exists-on-all?base=BTC&exchanges=BINANCE,OKX,BINGX
     */
    @GetMapping(value = "/symbol/exists-on-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> existsOnAll(
            @RequestParam String base,
            @RequestParam(required = false) String exchanges,
            @RequestParam(required = false, defaultValue = "FUTURE") SymbolType type
    ) {
        ExchangeName[] exs = parseExchangesParam(exchanges, type);
        Set<ExchangeName> exSet = Set.of(exs);
        boolean exists = symbolRegistryService.existsOnAll(exSet, base, type);
        Map<ExchangeName, String> symbols = symbolRegistryService.getSymbolsByExchange(exs, base, type);
        return Map.of(
                "base", base.toUpperCase(),
                "existsOnAll", exists,
                "exchanges", exSet.stream().map(ExchangeName::name).sorted().collect(Collectors.toList()),
                "symbols", symbols.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue))
        );
    }

    /**
     * List base assets that exist on all specified exchanges.
     * When no exchanges param is provided, uses all currently loaded exchanges.
     * Example: GET /future/symbol/common-bases  or  /future/symbol/common-bases?exchanges=BINANCE,OKX,BINGX
     */
    @GetMapping(value = "/symbol/common-bases", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getCommonBases(
            @RequestParam(required = false) String exchanges,
            @RequestParam(required = false, defaultValue = "FUTURE") SymbolType type
    ) {
        ExchangeName[] exs = parseExchangesParam(exchanges, type);
        Set<String> bases = symbolRegistryService.getBasesExistingOnAll(type, exs);
        return Map.of(
                "type", type.name(),
                "exchanges", Arrays.stream(exs).map(ExchangeName::name).sorted().collect(Collectors.toList()),
                "bases", bases.stream().sorted().collect(Collectors.toList())
        );
    }

    /** Parse comma-separated exchange names; when empty or null, use all loaded exchanges for the type. */
    private ExchangeName[] parseExchangesParam(String exchangesParam, SymbolType type) {
        if (exchangesParam != null && !exchangesParam.isBlank()) {
            ExchangeName[] exs = Arrays.stream(exchangesParam.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(ExchangeName::valueOf)
                    .toArray(ExchangeName[]::new);
            if (exs.length > 0) return exs;
        }
        Set<ExchangeName> loaded = symbolRegistryService.getLoadedExchanges(type);
        return loaded.toArray(new ExchangeName[0]);
    }

    /**
     * Reload symbol data from ExchangeInfo APIs (e.g. after adding a new exchange).
     */
    @GetMapping(value = "/symbol/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> refresh(@RequestParam(required = false, defaultValue = "FUTURE") SymbolType type) {
        return symbolRegistryService.loadRegistry(type)
                .map(r -> Map.of("status", "ok", "type", type.name(), "exchanges", String.valueOf(r.getExchanges().size())));
    }
}
