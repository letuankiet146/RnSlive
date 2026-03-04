package org.ltk.connector.requestor.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.ltk.connector.client.RESTApiUrl.BYBIT_BASE_WEBSOCKET_URI;

@Component
public class BybitFutureWebSocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BybitFutureWebSocket.class);
    private static final int RECONNECT_DELAY_SECONDS = 5;

    Supplier<WebsocketClientSpec.Builder> clientSpecSupplier = () -> WebsocketClientSpec.builder().maxFramePayloadLength(1024 * 1024);
    private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient(HttpClient.create(), clientSpecSupplier);

    public void subscribe(String subscribeMessage, Consumer<String> callback) {
        subscribe(BYBIT_BASE_WEBSOCKET_URI, subscribeMessage, callback);
    }

    public void subscribe(String uri, String subscribeMessage, Consumer<String> callback) {
        LOGGER.info("Subscribing to: {} with message {}", uri, subscribeMessage);
        webSocketClient.execute(
                URI.create(uri),
                session -> {
                    Flux<String> receiveMessages = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(callback)
                            .doOnError(this::errorConsume)
                            .doOnComplete(this::completion);
                    ;

                    if (StringUtils.hasText(subscribeMessage)){
                        Mono<WebSocketMessage> subscriptionMessage = Mono.just(
                                session.textMessage(subscribeMessage)
                        );
                        return session
                                .send(subscriptionMessage)
                                .thenMany(receiveMessages)
                                .doOnTerminate(onTerminate(uri, subscribeMessage, callback))
                                .then();
                    } else {
                        return receiveMessages
                                .doOnTerminate(onTerminate(uri, null, callback)).then();
                    }
                }
        ).subscribe();
    }

    private Runnable onTerminate(String uri, String subscribeMessage, Consumer<String> callback) {
        return () -> {
            LOGGER.info("WebSocket session terminated.");
            reconnect(uri, subscribeMessage, callback);
        };
    }


    // Handle reconnection logic with exponential backoff or fixed delay
    private void reconnect(String uri, String subscribeMessage, Consumer<String> callback) {
        LOGGER.info("Attempting to reconnect in {} seconds...", RECONNECT_DELAY_SECONDS);
        Mono.delay(Duration.ofSeconds(RECONNECT_DELAY_SECONDS))
                .doOnTerminate(()->{
                    LOGGER.info("Reconnecting...");
                    subscribe(uri, subscribeMessage, callback);
                })
                .subscribe();
    }


    // To stop the WebSocket client and clean up resources
    public void stopWebSocketClient() {
        LOGGER.info("Stopping WebSocket client...");
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
