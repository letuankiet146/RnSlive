package org.tk.rnslive.model.symbol;

import org.ltk.connector.client.ExchangeName;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds exchange-specific symbol strings per base asset (e.g. "BTC").
 * Built from ExchangeInfo API responses, filtered by {@link SymbolType}.
 * <p>
 * Example: getSymbol(OKX, "BTC") → "BTC-USDT-SWAP", getSymbol(BINANCE, "BTC") → "BTCUSDT".
 */
public class MultiExchangeSymbolRegistry {

    private final SymbolType symbolType;
    /** Per exchange: base asset (e.g. "BTC") → exchange symbol (e.g. "BTCUSDT" or "BTC-USDT-SWAP"). */
    private final Map<ExchangeName, Map<String, String>> exchangeToBaseToSymbol;

    public MultiExchangeSymbolRegistry(SymbolType symbolType, Map<ExchangeName, Map<String, String>> exchangeToBaseToSymbol) {
        this.symbolType = symbolType;
        Map<ExchangeName, Map<String, String>> copy = new HashMap<>();
        exchangeToBaseToSymbol.forEach((ex, map) -> copy.put(ex, Collections.unmodifiableMap(new HashMap<>(map))));
        this.exchangeToBaseToSymbol = Collections.unmodifiableMap(copy);
    }

    public SymbolType getSymbolType() {
        return symbolType;
    }

    /**
     * Returns the exchange-specific symbol for the given base asset (e.g. "BTC").
     *
     * @param exchange  the exchange
     * @param baseAsset base asset, e.g. "BTC"
     * @return exchange symbol (e.g. "BTCUSDT" for Binance, "BTC-USDT-SWAP" for OKX), or null if not listed
     */
    public String getSymbol(ExchangeName exchange, String baseAsset) {
        if (baseAsset == null) return null;
        Map<String, String> baseToSymbol = exchangeToBaseToSymbol.get(exchange);
        if (baseToSymbol == null) return null;
        return baseToSymbol.get(normalizeBase(baseAsset));
    }

    /**
     * Checks if the base asset exists on both given exchanges.
     */
    public boolean existsOnBoth(ExchangeName a, ExchangeName b, String baseAsset) {
        return getSymbol(a, baseAsset) != null && getSymbol(b, baseAsset) != null;
    }

    /**
     * Checks if the base asset exists on all given exchanges.
     */
    public boolean existsOnAll(Set<ExchangeName> exchanges, String baseAsset) {
        if (exchanges == null || exchanges.isEmpty()) return false;
        for (ExchangeName ex : exchanges) {
            if (getSymbol(ex, baseAsset) == null) return false;
        }
        return true;
    }

    /**
     * Returns the set of base assets that exist on all of the given exchanges.
     */
    public Set<String> getBasesExistingOnAll(ExchangeName... exchanges) {
        if (exchanges == null || exchanges.length == 0) return Set.of();
        Set<String> result = null;
        for (ExchangeName ex : exchanges) {
            Map<String, String> baseToSymbol = exchangeToBaseToSymbol.get(ex);
            if (baseToSymbol == null) return Set.of();
            Set<String> bases = baseToSymbol.keySet();
            if (result == null) result = new HashSet<>(bases);
            else result.retainAll(bases);
        }
        return result != null ? Collections.unmodifiableSet(result) : Set.of();
    }

    /**
     * Returns the set of base assets that exist on the given exchange.
     */
    public Set<String> getBasesForExchange(ExchangeName exchange) {
        Map<String, String> map = exchangeToBaseToSymbol.get(exchange);
        return map == null ? Set.of() : Collections.unmodifiableSet(map.keySet());
    }

    public Set<ExchangeName> getExchanges() {
        return Collections.unmodifiableSet(exchangeToBaseToSymbol.keySet());
    }

    private static String normalizeBase(String baseAsset) {
        return baseAsset == null ? null : baseAsset.toUpperCase().trim();
    }
}
