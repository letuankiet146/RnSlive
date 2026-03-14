package org.tk.rnslive.model.symbol;

/**
 * Type of trading pair when loading from exchange ExchangeInfo API.
 */
public enum SymbolType {
    /** Perpetual futures / swap (e.g. Binance USDT-M, OKX SWAP). */
    FUTURE,
    /** Spot pair. */
    SPOT
}
