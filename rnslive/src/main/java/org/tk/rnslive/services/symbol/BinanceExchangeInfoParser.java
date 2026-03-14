package org.tk.rnslive.services.symbol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.ExchangeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tk.rnslive.model.symbol.SymbolType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses Binance futures /fapi/v1/exchangeInfo or spot exchangeInfo.
 * Futures: symbol like "BTCUSDT", contractType "PERPETUAL", quoteAsset "USDT".
 * Spot: symbol like "BTCUSDT", no contractType, quoteAsset "USDT".
 */
@Component
public class BinanceExchangeInfoParser implements ExchangeInfoParser {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceExchangeInfoParser.class);
    private static final String QUOTE_USDT = "USDT";
    private static final String CONTRACT_TYPE_PERPETUAL = "PERPETUAL";

    private final ObjectMapper objectMapper;

    public BinanceExchangeInfoParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExchangeName getExchange() {
        return ExchangeName.BINANCE;
    }

    @Override
    public Map<String, String> parse(String exchangeInfoJson, SymbolType symbolType) {
        Map<String, String> baseToSymbol = new HashMap<>();
        if (exchangeInfoJson == null || exchangeInfoJson.isBlank()) return baseToSymbol;

        try {
            JsonNode root = objectMapper.readTree(exchangeInfoJson);
            JsonNode symbols = root.path("symbols");
            if (!symbols.isArray()) return baseToSymbol;

            for (JsonNode sym : symbols) {
                String quoteAsset = sym.path("quoteAsset").asText("");
                if (!QUOTE_USDT.equalsIgnoreCase(quoteAsset)) continue;

                if (symbolType == SymbolType.FUTURE) {
                    String contractType = sym.path("contractType").asText("");
                    if (!CONTRACT_TYPE_PERPETUAL.equalsIgnoreCase(contractType)) continue;
                } else if (symbolType == SymbolType.SPOT) {
                    if (sym.has("contractType") && !sym.path("contractType").asText("").isEmpty()) continue;
                }

                String baseAsset = sym.path("baseAsset").asText(null);
                String symbol = sym.path("symbol").asText(null);
                if (baseAsset != null && symbol != null) {
                    baseToSymbol.put(baseAsset.toUpperCase(), symbol);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to parse Binance exchangeInfo", e);
        }
        return baseToSymbol;
    }
}
