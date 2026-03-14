package org.tk.rnslive.services.symbol;

import org.ltk.connector.client.ExchangeName;
import org.tk.rnslive.model.symbol.SymbolType;

import java.util.Map;

/**
 * Parses raw ExchangeInfo/instruments API JSON into a map: base asset → exchange symbol.
 */
public interface ExchangeInfoParser {

    ExchangeName getExchange();

    /**
     * Parse JSON and return base asset (e.g. "BTC") → exchange symbol (e.g. "BTCUSDT" or "BTC-USDT-SWAP").
     *
     * @param exchangeInfoJson raw response from getExchangeInfo
     * @param symbolType       filter by FUTURE or SPOT
     * @return map of base to symbol; only USDT-quoted pairs are included
     */
    Map<String, String> parse(String exchangeInfoJson, SymbolType symbolType);
}
