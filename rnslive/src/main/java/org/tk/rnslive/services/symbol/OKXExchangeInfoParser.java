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
 * Parses OKX /api/v5/public/instruments response (e.g. instType=SWAP for perpetuals).
 * SWAP: instId "BTC-USDT-SWAP", baseCcy "BTC", quoteCcy "USDT".
 * SPOT: instType "SPOT", instId "BTC-USDT", baseCcy "BTC", quoteCcy "USDT".
 */
@Component
public class OKXExchangeInfoParser implements ExchangeInfoParser {

    private static final Logger LOG = LoggerFactory.getLogger(OKXExchangeInfoParser.class);
    private static final String QUOTE_USDT = "USDT";
    private static final String INST_TYPE_SWAP = "SWAP";
    private static final String INST_TYPE_SPOT = "SPOT";

    private final ObjectMapper objectMapper;

    public OKXExchangeInfoParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExchangeName getExchange() {
        return ExchangeName.OKX;
    }

    @Override
    public Map<String, String> parse(String exchangeInfoJson, SymbolType symbolType) {
        Map<String, String> baseToSymbol = new HashMap<>();
        if (exchangeInfoJson == null || exchangeInfoJson.isBlank()) return baseToSymbol;

        try {
            JsonNode root = objectMapper.readTree(exchangeInfoJson);
            JsonNode data = root.path("data");
            if (!data.isArray()) return baseToSymbol;

            String requiredInstType = symbolType == SymbolType.FUTURE ? INST_TYPE_SWAP : INST_TYPE_SPOT;

            for (JsonNode inst : data) {
                String instType = inst.path("instType").asText("");
                if (!requiredInstType.equalsIgnoreCase(instType)) continue;

                String quoteCcy = inst.path("settleCcy").asText("");
                if (!QUOTE_USDT.equalsIgnoreCase(quoteCcy)) continue;

                String baseCcy = inst.path("ctValCcy").asText(null);
                String instId = inst.path("instId").asText(null);
                if (baseCcy != null && instId != null) {
                    baseToSymbol.put(baseCcy.toUpperCase(), instId);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to parse OKX instruments", e);
        }
        return baseToSymbol;
    }
}
