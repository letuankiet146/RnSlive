package org.ltk.connector.service.impl.exchange;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.client.impl.BingXExFutureClientImpl;
import org.ltk.model.exchange.order.Order;
import org.ltk.model.exchange.order.OrderType;
import org.ltk.model.exchange.order.SideOrder;
import org.ltk.model.exchange.order.bingx.BingXOrder;
import org.ltk.model.exchange.position.Position;
import org.ltk.model.exchange.position.bingx.BingXPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

@Service
public class BingXService {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private BingXExFutureClientImpl exFutureClient;

    private TreeMap<String, Object> buildCreateOrderParams(String symbol, SideOrder side, double quantity) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("side", side);
        sortedParams.put("quantity", BigDecimal.valueOf(quantity));
        return sortedParams;
    }

    public Order createLimitOrder(String symbol, SideOrder side, double price, double quantity) {
        TreeMap<String, Object> sortedParams = buildCreateOrderParams(symbol, side, quantity);
        sortedParams.put("type", OrderType.LIMIT.name());
        sortedParams.put("price", BigDecimal.valueOf(price));
        String positionSide = sortedParams.get("side").toString().equals("BUY") ? "LONG" : "SHORT";
        sortedParams.put("positionSide", positionSide);
        var response =  exFutureClient.placeOrder(sortedParams);
        try {
            String orderJson = mapper.readTree(response).path("data").path("order").toString();
            if (StringUtils.hasText(orderJson)) {
                return mapper.readValue(orderJson, BingXOrder.class);
            }
            throw new RuntimeException("Error creating order: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
    public Order createMarketOrder(String symbol, SideOrder side, double quantity) {
        TreeMap<String, Object> sortedParams = buildCreateOrderParams(symbol, side, quantity);
        sortedParams.put("type", OrderType.MARKET.name());
        String positionSide = sortedParams.get("side").toString().equals("BUY") ? "LONG" : "SHORT";
        sortedParams.put("positionSide", positionSide);
        var response =  exFutureClient.placeOrder(sortedParams);
        try {
            String orderJson = mapper.readTree(response).path("data").path("order").toString();
            if (StringUtils.hasText(orderJson)) {
                return mapper.readValue(orderJson, BingXOrder.class);
            }
            throw new RuntimeException("Error creating order: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private TreeMap<String, Object> buildMarkerStopParams(String symbol, SideOrder side, double price, double stopPrice, double quantity) {
        TreeMap<String, Object> sortedParams = buildTakerStopParams(symbol, side, stopPrice, quantity);
        sortedParams.put("price", BigDecimal.valueOf(price));
        return sortedParams;
    }
    private TreeMap<String, Object> buildTakerStopParams(String symbol, SideOrder side, double stopPrice, double quantity) {
        TreeMap<String, Object> sortedParams = buildCreateOrderParams(symbol, side, quantity);
        if (stopPrice > 0) {
            sortedParams.put("stopPrice", BigDecimal.valueOf(stopPrice));
        }
        return sortedParams;
    }

    public Order createTakeProfitOrder(String symbol, SideOrder side, double price, double quantity) {
        String positionSide = side.name().equals("BUY") ? "SHORT" : "LONG";
        TreeMap<String, Object> sortedParams = buildMarkerStopParams(symbol, side, price, price, quantity);
        sortedParams.put("type", OrderType.TAKE_PROFIT.name());
        sortedParams.put("positionSide", positionSide);
        var response =  exFutureClient.placeOrder(sortedParams);
        try {
            String orderJson = mapper.readTree(response).path("data").path("order").toString();
            if (StringUtils.hasText(orderJson)) {
                return mapper.readValue(orderJson, BingXOrder.class);
            }
            throw new RuntimeException("Error creating order: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public Order createStopLossOrder(String symbol, SideOrder side, double price, double quantity) {
        String positionSide = side.name().equals("BUY") ? "SHORT" : "LONG";
        TreeMap<String, Object> sortedParams = buildMarkerStopParams(symbol, side, price, price, quantity);
        sortedParams.put("type", OrderType.STOP.name());
        sortedParams.put("positionSide", positionSide);
        var response =  exFutureClient.placeOrder(sortedParams);
        try {
            String orderJson = mapper.readTree(response).path("data").path("order").toString();
            if (StringUtils.hasText(orderJson)) {
                return mapper.readValue(orderJson, BingXOrder.class);
            }
            throw new RuntimeException("Error creating order: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelOrder(String symbol, String orderId) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("orderId", orderId);
        var response = exFutureClient.deleteOrder(sortedParams);
        try {
            String code = mapper.readTree(response).path("code").asText();
            if (!StringUtils.hasText(code) || Integer.parseInt(code) != 0) {
                throw new RuntimeException("Error canceling order: " + response);
            }
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
    public List<Order> createOrders(String symbol, Set<BingXOrder> orders) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        try {
            String json = mapper.writeValueAsString(orders);
            sortedParams.put("batchOrders", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        var response = exFutureClient.placeMultiOrder(sortedParams);
        try {
            String orderJson = mapper.readTree(response).path("data").path("orders").toString();
            if (StringUtils.hasText(orderJson)) {
                return Arrays.asList(mapper.readValue(orderJson, BingXOrder[].class));
            }
            throw new RuntimeException("Error creating order: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelOrder(String symbol, Set<Long> orderIds) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("orderIdList", orderIds.toString());
        var response = exFutureClient.deleteMultiOrder(sortedParams);
        try {
            String code = mapper.readTree(response).path("code").asText();
            if (!StringUtils.hasText(code) || Integer.parseInt(code) != 0) {
                throw new RuntimeException("Error canceling order: " + response);
            }
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelAllOpenOrders(String symbol) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        var response = exFutureClient.cancelAllOpenOrders(sortedParams);
        try {
            String code = mapper.readTree(response).path("code").asText();
            if (!StringUtils.hasText(code) || Integer.parseInt(code) != 0) {
                throw new RuntimeException("Error canceling orders: " + response);
            }
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeAllPosition(String symbol) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        var response = exFutureClient.closeAllPosition(sortedParams);
        try {
            String code = mapper.readTree(response).path("code").asText();
            if (!StringUtils.hasText(code) || Integer.parseInt(code) != 0) {
                throw new RuntimeException("Error closing position: " + response);
            }
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Order> getOpenOrders(String symbol) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        var response =  exFutureClient.getOpenOrders(sortedParams);
        try {
            String orderJson = mapper.readTree(response).path("data").path("orders").toString();
            if (StringUtils.hasText(orderJson)) {
                return Arrays.asList(mapper.readValue(orderJson, BingXOrder[].class));
            }
            throw new RuntimeException("Error creating order: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Position> getPosition(String symbol) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        return getPositionList(sortedParams);
    }

    private  List<Position> getPositionList(TreeMap<String, Object> sortedParams) {
        var response =  exFutureClient.getPosition(sortedParams);
        try {
            String positionJson = mapper.readTree(response).path("data").toString();
            if (StringUtils.hasText(positionJson)) {
                return Arrays.asList(mapper.readValue(positionJson, BingXPosition[].class));
            }
            throw new RuntimeException("Error getting position: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public  List<Position> getPositionHistory(String symbol, String positionId) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("positionId", positionId);
        var response =  exFutureClient.getPositionHistory(sortedParams);
        try {
            String positionJson = mapper.readTree(response).path("data").path("positionHistory").toString();
            if (StringUtils.hasText(positionJson)) {
                return Arrays.asList(mapper.readValue(positionJson, BingXPosition[].class));
            }
            throw new RuntimeException("Error getting position: " + response);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public double getMarkPrice(String symbol) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        var response =  exFutureClient.getPremiumIndex(sortedParams);
        try {
            String markPrice = mapper.readValue(mapper.readTree(response).path("data").path("markPrice").toString(), String.class);
            return Double.parseDouble(markPrice);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLeverage(String symbol, int leverage, String positionSide) {
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        sortedParams.put("symbol", symbol);
        sortedParams.put("side", positionSide);
        sortedParams.put("leverage", leverage);
        exFutureClient.setLeverage(sortedParams);
    }

    public void subscribeMarkPrice(String symbol, Consumer<String> callback) {
        exFutureClient.subscribeMarkPrice(symbol, null, callback);
    }

    public void subscribeDepth(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeDepth(symbol, interval, callback);
    }

    public void subscribeTradeDetail(String symbol, String interval, Consumer<String> callback) {
        exFutureClient.subscribeTradeDetail(symbol, interval, callback);
    }

    public void subscribeAccountData(Consumer<String> callback) {
        exFutureClient.subscribeAccountData(callback);
    }

}
