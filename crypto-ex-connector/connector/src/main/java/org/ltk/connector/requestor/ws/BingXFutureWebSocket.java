package org.ltk.connector.requestor.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ltk.connector.requestor.api.BingXFutureRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static org.ltk.connector.client.RESTApiUrl.BINGX_BASE_WEBSOCKET_URI;
import static org.ltk.connector.client.RESTApiUrl.BINGX_GET_LISTEN_KEY_URL;

@Component
public class BingXFutureWebSocket extends BaseFutureWebsocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BingXFutureWebSocket.class);
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private String LISTEN_KEY;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Timer timer;

    @Autowired
    private BingXFutureRequester requester;

    private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();

    private void refreshListenKey(String apiKey) {
        String getListenKeyResponse = requester.sendRequest(HttpMethod.POST, BINGX_GET_LISTEN_KEY_URL, apiKey);
        try {
            LISTEN_KEY = mapper.readValue(mapper.readTree(getListenKeyResponse).path("listenKey").toString(), String.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribeAccountData(String apiKey, String subscribeMessage, Consumer<String> callback) {
        refreshListenKey(apiKey);
        String accountDataURI = String.format("%s?listenKey=%s",BINGX_BASE_WEBSOCKET_URI, LISTEN_KEY);
        LOGGER.info("Subscribing to account data: {}", accountDataURI);
        subscribe(accountDataURI, subscribeMessage, callback);
    }

    @Override
    public void subscribe(String subscribeMessage, Consumer<String> callback) {
        subscribe(BINGX_BASE_WEBSOCKET_URI, subscribeMessage, callback);
    }

    public void subscribe(String uri, String subscribeMessage, Consumer<String> callback) {
        LOGGER.info("Subscribing to: {} with message {}", uri, subscribeMessage);
        Disposable connection = webSocketClient.execute(
                URI.create(uri),
                session -> {
                    // 2. Receive and process incoming messages
                    Flux<byte[]> receiveMessages = session.receive()
                            .map(WebSocketMessage::getPayload)
                            .map(this::dataBufferToByteArray)
                            .doOnNext(incomeMessageProcess(callback, session))
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
        connectionCallbacks.putIfAbsent(subscribeMessage, callback);
    }

    private Runnable onTerminate(String uri, String subscribeMessage, Consumer<String> callback) {
        return () -> {
            LOGGER.info("WebSocket session terminated.");
//            reconnect(uri, subscribeMessage, callback);
        };
    }

    private static Consumer<byte[]> incomeMessageProcess(Consumer<String> callback, WebSocketSession session) {
        return dataBuffer -> {
            try {
                String message = decodeGzip(dataBuffer);
                if ("Ping".equals(message)) {
                    var pongWS = session.pongMessage(pong -> {
                        ByteBuffer pingPayload = ByteBuffer.wrap("Pong".getBytes());
                        return session.bufferFactory().wrap(pingPayload);
                    });
                    // send a Pong response
                    session.send(Mono.just(pongWS)).subscribe();
                } else {
                    callback.accept(message);
                }
            } catch (IOException e) {
                LOGGER.error("Error decoding message: {}", e.getMessage());
            }
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

    private static String decodeGzip(byte[] compressedData) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedData));
        byte[] buffer = new byte[1024];
        StringBuilder decodedMessage = new StringBuilder();

        int len;
        while ((len = gzipInputStream.read(buffer)) != -1) {
            decodedMessage.append(new String(buffer, 0, len));
        }

        gzipInputStream.close();
        return decodedMessage.toString();
    }
}
