package org.ltk.connector.exception;

public class ExchangeClientException extends RuntimeException {
    public ExchangeClientException(String message) {
        super(message);
    }

    public ExchangeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
