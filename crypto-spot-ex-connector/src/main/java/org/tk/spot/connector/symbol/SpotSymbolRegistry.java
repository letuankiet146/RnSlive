package org.tk.spot.connector.symbol;

import org.tk.spot.connector.client.ExchangeName;

import java.util.Map;
import java.util.Set;

public record SpotSymbolRegistry(
        Map<ExchangeName, Map<String, String>> baseToSymbolByExchange
) {
    public Set<ExchangeName> getExchanges() {
        return baseToSymbolByExchange.keySet();
    }

    public String getSymbol(ExchangeName exchange, String baseUpper) {
        Map<String, String> m = baseToSymbolByExchange.get(exchange);
        if (m == null) return null;
        return m.get(baseUpper);
    }

    public boolean existsOnAll(Set<ExchangeName> exchanges, String baseUpper) {
        for (ExchangeName ex : exchanges) {
            if (getSymbol(ex, baseUpper) == null) return false;
        }
        return true;
    }

    public boolean existsOnBoth(ExchangeName a, ExchangeName b, String baseUpper) {
        return getSymbol(a, baseUpper) != null && getSymbol(b, baseUpper) != null;
    }
}

