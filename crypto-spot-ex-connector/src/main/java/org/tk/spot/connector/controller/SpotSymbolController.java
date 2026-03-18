package org.tk.spot.connector.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tk.spot.connector.client.ExchangeName;
import org.tk.spot.connector.symbol.SpotSymbolRegistryService;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/spot")
public class SpotSymbolController {

    private final SpotSymbolRegistryService symbolRegistryService;

    public SpotSymbolController(SpotSymbolRegistryService symbolRegistryService) {
        this.symbolRegistryService = symbolRegistryService;
    }

    /**
     * List exchanges that have symbol data loaded.
     * Example: GET /spot/symbol/exchanges
     */
    @GetMapping(value = "/symbol/exchanges", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getLoadedExchanges() {
        Set<ExchangeName> exchanges = symbolRegistryService.getLoadedExchanges();
        return Map.of(
                "exchanges", exchanges.stream().map(Enum::name).sorted().collect(Collectors.toList())
        );
    }

    /**
     * Get exchange-specific symbol for a base asset.
     * Example: GET /spot/symbol?exchange=OKX&base=BTC → "BTC-USDT"
     *          GET /spot/symbol?exchange=BINANCE&base=BTC → "BTCUSDT"
     */
    @GetMapping(value = "/symbol", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getSymbol(
            @RequestParam ExchangeName exchange,
            @RequestParam String base
    ) {
        String symbol = symbolRegistryService.getSymbol(exchange, base);
        return Map.of(
                "exchange", exchange.name(),
                "base", base == null ? "" : base.toUpperCase(),
                "symbol", symbol != null ? symbol : ""
        );
    }

    /**
     * Check if a base exists on both given exchanges.
     * Example: GET /spot/symbol/exists-on-both?base=BTC&a=BINANCE&b=OKX
     */
    @GetMapping(value = "/symbol/exists-on-both", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> existsOnBoth(
            @RequestParam String base,
            @RequestParam ExchangeName a,
            @RequestParam ExchangeName b
    ) {
        boolean exists = symbolRegistryService.getRegistry().existsOnBoth(a, b, base.toUpperCase());
        return Map.of(
                "base", base.toUpperCase(),
                "existsOnBoth", exists,
                "symbols", symbolRegistryService.getSymbolsByExchange(new ExchangeName[]{a, b}, base)
        );
    }

    /**
     * Check if a base exists on all given exchanges and return symbol per exchange.
     * When no exchanges param is provided, uses all currently loaded exchanges.
     * Example: GET /spot/symbol/exists-on-all?base=BTC  or  /spot/symbol/exists-on-all?base=BTC&exchanges=BINANCE,OKX
     */
    @GetMapping(value = "/symbol/exists-on-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> existsOnAll(
            @RequestParam String base,
            @RequestParam(required = false) String exchanges
    ) {
        ExchangeName[] exs = parseExchangesParam(exchanges);
        Set<ExchangeName> exSet = Set.of(exs);
        boolean exists = symbolRegistryService.existsOnAll(exSet, base);
        Map<ExchangeName, String> symbols = symbolRegistryService.getSymbolsByExchange(exs, base);
        return Map.of(
                "base", base.toUpperCase(),
                "existsOnAll", exists,
                "exchanges", exSet.stream().map(Enum::name).sorted().collect(Collectors.toList()),
                "symbols", symbols.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue))
        );
    }

    /**
     * List base assets that exist on all specified exchanges.
     * When no exchanges param is provided, uses all currently loaded exchanges.
     * Example: GET /spot/symbol/common-bases  or  /spot/symbol/common-bases?exchanges=BINANCE,OKX
     */
    @GetMapping(value = "/symbol/common-bases", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getCommonBases(
            @RequestParam(required = false) String exchanges
    ) {
        ExchangeName[] exs = parseExchangesParam(exchanges);
        Set<String> bases = symbolRegistryService.getBasesExistingOnAll(exs);
        return Map.of(
                "exchanges", Arrays.stream(exs).map(Enum::name).sorted().collect(Collectors.toList()),
                "bases", bases.stream().sorted().collect(Collectors.toList())
        );
    }

    /**
     * Reload symbol data from public ExchangeInfo APIs.
     */
    @GetMapping(value = "/symbol/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> refresh() {
        return symbolRegistryService.loadRegistry()
                .map(r -> Map.of("status", "ok", "exchanges", String.valueOf(r.getExchanges().size())));
    }

    /** Parse comma-separated exchange names; when empty or null, use all loaded exchanges. */
    private ExchangeName[] parseExchangesParam(String exchangesParam) {
        if (exchangesParam != null && !exchangesParam.isBlank()) {
            ExchangeName[] exs = Arrays.stream(exchangesParam.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(ExchangeName::valueOf)
                    .toArray(ExchangeName[]::new);
            if (exs.length > 0) return exs;
        }
        Set<ExchangeName> loaded = symbolRegistryService.getLoadedExchanges();
        return loaded.toArray(new ExchangeName[0]);
    }
}

