package org.tk.spot.connector.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Shared WebSocket client for spot connectors. Tracks subscriptions by key so they can be
 * stopped with {@link #disconnect(String)}. Re-subscribing the same key closes the previous
 * connection first to avoid duplicate connections and memory growth.
 */
@Component
public class SpotWebSocketSupport {

    private static final Logger LOG = LoggerFactory.getLogger(SpotWebSocketSupport.class);
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    private final ConcurrentHashMap<String, Disposable> subscriptions = new ConcurrentHashMap<>();

    /**
     * Connect to the WebSocket and track by key. If this key already has a subscription,
     * it is disconnected first. Use {@link #disconnect(String)} to stop later.
     *
     * @param key              unique subscription id (e.g. "BINANCE:btcusdt"); same key used to disconnect
     * @param uri              WebSocket URI
     * @param subscribeMessage optional message to send after connect (can be null)
     * @param callback         receives each text message as String
     */
    public void connect(String key, String uri, String subscribeMessage, Consumer<String> callback) {
        disconnect(key);
        LOG.info("Connecting to {} key={}", uri, key);
        Disposable d = client.execute(
                URI.create(uri),
                session -> {
                    Flux<String> incoming = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(callback::accept)
                            .doOnError(e -> LOG.error("WebSocket error key={}: {}", key, e.getMessage()))
                            .doOnComplete(() -> LOG.info("WebSocket completed key={}", key));

                    if (subscribeMessage != null && !subscribeMessage.isBlank()) {
                        return session.send(Mono.just(session.textMessage(subscribeMessage)))
                                .thenMany(incoming)
                                .then();
                    }
                    return incoming.then();
                }
        ).subscribe(
                null,
                e -> {
                    subscriptions.remove(key);
                    LOG.warn("WebSocket failed key={}, reconnecting in {}s: {}", key, RECONNECT_DELAY_SECONDS, e.getMessage());
                    Mono.delay(Duration.ofSeconds(RECONNECT_DELAY_SECONDS))
                            .subscribe(t -> connect(key, uri, subscribeMessage, callback));
                },
                () -> subscriptions.remove(key)
        );
        subscriptions.put(key, d);
    }

    /**
     * Stop the WebSocket subscription for this key and release the connection.
     * No-op if the key is not subscribed. Safe to call multiple times.
     */
    public void disconnect(String key) {
        Disposable d = subscriptions.remove(key);
        if (d != null) {
            d.dispose();
            LOG.info("Disconnected WebSocket key={}", key);
        }
    }
}
