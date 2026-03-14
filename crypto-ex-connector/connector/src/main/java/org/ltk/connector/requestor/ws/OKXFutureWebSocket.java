package org.ltk.connector.requestor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.ltk.connector.client.RESTApiUrl.OKX_BASE_WEBSOCKET_URI;

@Component
public class OKXFutureWebSocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OKXFutureWebSocket.class);
    private static final int RECONNECT_DELAY_SECONDS = 2;

    private String LISTEN_KEY;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Timer timer;

    private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();

    /** Per-key subscription state for manual disconnect and reconnect. */
    private final ConcurrentHashMap<String, SubscriptionHandle> subscriptions = new ConcurrentHashMap<>();

    /**
     * Subscription handle holding connection disposable and params for reconnect.
     */
    private static final class SubscriptionHandle {
        final String uri;
        final String subscribeMessage;
        final Consumer<String> callback;
        volatile Disposable disposable;

        SubscriptionHandle(String uri, String subscribeMessage, Consumer<String> callback) {
            this.uri = uri;
            this.subscribeMessage = subscribeMessage;
            this.callback = callback;
        }

        void dispose() {
            if (disposable != null) {
                disposable.dispose();
                disposable = null;
            }
        }
    }

    public void subscribe(String subscribeMessage, Consumer<String> callback) {
        LOGGER.info("Subscribing to account data: {}", OKX_BASE_WEBSOCKET_URI);
        subscribe(null, OKX_BASE_WEBSOCKET_URI, subscribeMessage, callback);
    }

    /** Subscribes with a key so this subscription can be disconnected manually via {@link #disconnect(String)}. */
    public void subscribe(String key, String subscribeMessage, Consumer<String> callback) {
        subscribe(key, OKX_BASE_WEBSOCKET_URI, subscribeMessage, callback);
    }

    /**
     * Subscribe to OKX WebSocket. If {@code key} is non-null, the subscription can be interrupted
     * with {@link #disconnect(String)}; on session end (or after disconnect) it will reconnect automatically.
     */
    public void subscribe(String key, String uri, String subscribeMessage, Consumer<String> callback) {
        String k = (key != null && !key.isEmpty()) ? key : ("_default_" + System.identityHashCode(callback));
        SubscriptionHandle handle = new SubscriptionHandle(uri, subscribeMessage, callback);
        subscriptions.put(k, handle);

        LOGGER.info("Subscribing to: {} with message {}", uri, subscribeMessage);
        Disposable d = webSocketClient.execute(
                URI.create(uri),
                session -> {
                    Flux<String> receiveMessages = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(callback::accept)
                            .doOnError(this::errorConsume)
                            .doOnComplete(this::completion);

                    if (StringUtils.hasText(subscribeMessage)) {
                        Mono<WebSocketMessage> subscriptionMessage = Mono.just(
                                session.textMessage(subscribeMessage)
                        );
                        return session
                                .send(subscriptionMessage)
                                .thenMany(receiveMessages)
                                .doOnTerminate(onTerminate(k))
                                .then();
                    } else {
                        return receiveMessages
                                .doOnTerminate(onTerminate(k))
                                .then();
                    }
                }
        ).subscribe();
        handle.disposable = d;
    }

    private Runnable onTerminate(String key) {
        return () -> {
            LOGGER.info("WebSocket session terminated for key: {}", key);
            reconnect(key);
        };
    }

    private void reconnect(String key) {
        SubscriptionHandle handle = subscriptions.get(key);
        if (handle == null) {
            return;
        }
        LOGGER.info("Attempting to reconnect in {} seconds for key: {}...", RECONNECT_DELAY_SECONDS, key);
        Mono.delay(Duration.ofSeconds(RECONNECT_DELAY_SECONDS))
                .doOnTerminate(() -> {
                    if (subscriptions.containsKey(key)) {
                        LOGGER.info("Reconnecting for key: {}", key);
                        subscribe(key, handle.uri, handle.subscribeMessage, handle.callback);
                    }
                })
                .subscribe();
    }

    /**
     * Manually interrupt the WebSocket connection for the given key. The connector will
     * reconnect after a short delay and resubscribe, so a fresh snapshot will be received (e.g. for OKX books).
     */
    public void disconnect(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        SubscriptionHandle handle = subscriptions.get(key);
        if (handle != null) {
            LOGGER.info("Manually disconnecting WebSocket for key: {}", key);
            handle.dispose();
        }
    }

    /** Stop the WebSocket client and clean up resources. */
    public void stopWebSocketClient() {
        LOGGER.info("Stopping WebSocket client...");
        subscriptions.values().forEach(SubscriptionHandle::dispose);
        subscriptions.clear();
    }

    private void errorConsume(Throwable error) {
        LOGGER.error("Error occurred: {}", error.getMessage());
    }

    private void completion() {
        LOGGER.info("WebSocket session completed.");
    }

    private byte[] dataBufferToByteArray(DataBuffer dataBuffer) {
        byte[] byteArray = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(byteArray);  // Copy the DataBuffer content to byte[]
        return byteArray;
    }
}
