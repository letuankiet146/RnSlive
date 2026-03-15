# crypto-spot-ex-connector

**Library** (no REST API): Spring Boot WebFlux-based connector for **Binance**, **OKX**, **BingX**, and **BitMEX** spot/mark price streaming. Use the service classes in your own project. All responses are **String** (exchange-specific JSON).

## Features

- **subscribeMarkPrice(exchangeName, symbol, callback)** – subscribe to mark/spot price; each update is passed to the callback as a `String`.
- Implementations follow each exchange’s official WebSocket documentation.
- Packaged as a JAR; no executable main, no REST controller.

## Tech Stack

- Java 17  
- Spring Boot 3.3.4  
- Spring WebFlux (Reactor Netty)  
- Jackson  

## Project Layout

```
src/main/java/org/tk/spot/connector/
├── SpotConnectorApplication.java   # Config only (for component scanning)
├── client/
│   ├── ExchangeName.java
│   ├── SpotApiUrls.java
│   ├── SpotMarkPriceClient.java
│   └── impl/
│       ├── BinanceSpotClientImpl.java
│       ├── OKXSpotClientImpl.java
│       ├── BingXSpotClientImpl.java
│       └── BitMEXSpotClientImpl.java
├── ws/
│   └── SpotWebSocketSupport.java
├── service/
│   ├── SpotExchangeService.java
│   └── impl/
│       └── SpotExchangeServiceImpl.java
```

## Use as a library

### 1. Add dependency

```xml
<dependency>
    <groupId>org.tk</groupId>
    <artifactId>crypto-spot-ex-connector</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Enable component scanning (or import config)

In your Spring Boot application, ensure the connector package is scanned, e.g.:

```java
@SpringBootApplication(scanBasePackages = {"your.package", "org.tk.spot.connector"})
public class YourApplication { ... }
```

Or:

```java
@Import(SpotConnectorApplication.class)
```

### 3. Inject and use the service

```java
@Autowired
private SpotExchangeService spotExchangeService;

// Subscribe (re-subscribing the same channel closes the previous connection first)
spotExchangeService.subscribeMarkPrice(ExchangeName.BINANCE, "btcusdt", msg -> System.out.println(msg));
spotExchangeService.subscribeMarkPrice(ExchangeName.OKX, "BTC-USDT", msg -> System.out.println(msg));

// Stop a subscription when done (avoids leaving WebSocket connections open)
spotExchangeService.unsubscribeMarkPrice(ExchangeName.BINANCE, "btcusdt");
spotExchangeService.unsubscribeMarkPrice(ExchangeName.OKX, "BTC-USDT");
```

- **Re-subscribe same channel**: If you call `subscribeMarkPrice` again for the same exchange+symbol, the previous WebSocket is disconnected first, then a new connection is created. So you do not accumulate duplicate connections or memory.
- **Unsubscribe**: Call `unsubscribeMarkPrice(exchangeName, symbol)` with the same symbol format as for subscribe. No-op if that channel was not subscribed. Safe to call multiple times.

## Build and test

```bash
mvn clean install
mvn test
```

Tests include unit tests for `SpotExchangeServiceImpl` and each client impl (Binance, OKX, BingX, BitMEX), plus a Spring context test to ensure the library loads correctly.

## Exchange details (official docs)

| Exchange | WebSocket docs | Symbol format |
|----------|----------------|---------------|
| **Binance** | [Web Socket Streams](https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams) | `btcusdt` (lowercase) |
| **OKX** | [Tickers channel](https://www.okx.com/docs-v5/en/#websocket-api-public-channel-tickers-channel) | `BTC-USDT` |
| **BingX** | [Spot API](https://bingx-api.github.io/docs) | `BTC-USDT` |
| **BitMEX** | [WebSocket API](https://docs.bitmex.com/app/wsAPI) | `XBTUSD` (derivatives) |
