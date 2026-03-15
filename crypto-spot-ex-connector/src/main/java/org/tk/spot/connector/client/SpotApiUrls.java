package org.tk.spot.connector.client;

/**
 * Official WebSocket and REST base URLs for spot market data.
 * References:
 * - Binance: https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams
 * - OKX:     https://www.okx.com/docs-v5/en/#websocket-api-public-channel-tickers-channel
 * - BingX:   https://bingx-api.github.io/docs (spot market API)
 * - BitMEX:  https://docs.bitmex.com (derivatives; no spot - use instrument/quote for mark price)
 */
public final class SpotApiUrls {

    private SpotApiUrls() {}

    /** Binance Spot WebSocket base. Stream: &lt;symbol&gt;@ticker (e.g. btcusdt@ticker). */
    public static final String BINANCE_SPOT_WS = "wss://stream.binance.com:9443/ws";

    /** OKX Public WebSocket (spot tickers channel: tickers, instId e.g. BTC-USDT). */
            public static final String OKX_PUBLIC_WS = "wss://ws.okx.com:8443/ws/v5/public";

    /** BingX Spot WebSocket (market data). Pattern aligned with swap: open-api-swap.bingx.com/swap-market. */
    public static final String BINGX_SPOT_WS = "wss://open-api.bingx.com/spot-market";

    /** BitMEX WebSocket (derivatives; subscribe instrument/quote for mark price). */
    public static final String BITMEX_WS = "wss://ws.bitmex.com/realtime";
}
