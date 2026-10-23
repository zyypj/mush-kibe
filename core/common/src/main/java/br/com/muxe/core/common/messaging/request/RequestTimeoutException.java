package br.com.muxe.core.common.messaging.request;

import java.time.Duration;

public final class RequestTimeoutException extends RuntimeException {

    public RequestTimeoutException(String type, Duration timeout) {
        super("No response to " + type + " within " + timeout.toMillis() + " ms");
    }
}
