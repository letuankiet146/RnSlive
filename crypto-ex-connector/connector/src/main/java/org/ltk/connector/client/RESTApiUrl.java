package org.ltk.connector.client;

public class RESTApiUrl {
    public static final String BINGX_SET_LEVERAGE_URL = "/openApi/swap/v2/trade/leverage";
    public static final String BINGX_PREMIUM_INDEX_URL = "/openApi/swap/v2/quote/premiumIndex";
    public static final String BINGX_PLACE_ORDER_URL = "/openApi/swap/v2/trade/order";
    public static final String BINGX_PLACE_MULTI_ORDER_URL = "/openApi/swap/v2/trade/batchOrders";
    public static final String BINGX_QUERY_ALL_OPEN_ORDERS_URL = "/openApi/swap/v2/trade/openOrders";
    public static final String BINGX_CANCEL_ALL_OPEN_ORDERS_URL = "/openApi/swap/v2/trade/allOpenOrders";
    public static final String BINGX_POSITION_URL = "/openApi/swap/v2/user/positions";
    public static final String BINGX_POSITION_HISTORY_URL = "/openApi/swap/v1/trade/positionHistory";
    public static final String BINGX_CLOSE_ALL_POSITION_URL = "/openApi/swap/v2/trade/closeAllPositions";
    public static final String BINGX_GET_LISTEN_KEY_URL = "/openApi/user/auth/userDataStream";

    public static final String OKX_GET_DEPTH_FULL_URL = "/api/v5/market/books-full";
    public static final String BINANCE_GET_DEPTH_URL = "/fapi/v1/depth";

    //WSS
    public static final String BINGX_BASE_WEBSOCKET_URI = "wss://open-api-swap.bingx.com/swap-market";
    public static final String BINANCE_BASE_WEBSOCKET_URI = "wss://fstream.binance.com/ws";
    public static final String BYBIT_BASE_WEBSOCKET_URI = "wss://stream.bybit.com/v5/public/linear";
    public static final String OKX_BASE_WEBSOCKET_URI = "wss://ws.okx.com:8443/ws/v5/public";

}
