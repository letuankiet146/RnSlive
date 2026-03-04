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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Timer;
import java.util.function.Consumer;

import static org.ltk.connector.client.RESTApiUrl.BINANCE_BASE_WEBSOCKET_URI;

@Component
public class BinanceFutureWebSocket extends BaseFutureWebsocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinanceFutureWebSocket.class);
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private String LISTEN_KEY;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Timer timer;

    private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();

    @Override
    public void subscribe(String path, Consumer<String> callback) {
        String accountDataURI = String.format("%s/%s",BINANCE_BASE_WEBSOCKET_URI, path);
        subscribe(accountDataURI, null, callback);
        connectionCallbacks.putIfAbsent(path, callback);
    }

    public void subscribe(String uri, String subscribeMessage, Consumer<String> callback) {
        LOGGER.info("Subscribing to: {} with message {}", uri, subscribeMessage);
        var connection = webSocketClient.execute(
                URI.create(uri),
                session -> {
                    // 2. Receive and process incoming messages
                    Flux<String> receiveMessages = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(message -> {
                                callback.accept(message);
                            })
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
        connections.add(connection);
    }

    private Runnable onTerminate(String uri, String subscribeMessage, Consumer<String> callback) {
        return () -> {
            LOGGER.info("WebSocket session terminated.");
//            reconnect(uri, subscribeMessage, callback);
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
