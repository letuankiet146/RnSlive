package org.ltk.connector.requestor.ws;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;

import java.util.*;
import java.util.function.Consumer;

public abstract class BaseFutureWebsocket {
    protected final Map<String, Consumer<String>> connectionCallbacks = new HashMap<>();
    protected final List<Disposable> connections = new ArrayList<>();

    @Autowired
    private Timer timer;

    @PostConstruct
    public void init() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean isNeedReconnect = connections.stream().anyMatch(Disposable::isDisposed);
                if (isNeedReconnect) {
                    connections.forEach(Disposable::dispose);
                    connections.clear();
                    for (Map.Entry<String, Consumer<String>> entry : connectionCallbacks.entrySet()) {
                        subscribe(entry.getKey(), entry.getValue());
                    }
                }
            }
        }, 0, 1000 * 30);
    }

    public abstract void subscribe(String key, Consumer<String> callback);
}
