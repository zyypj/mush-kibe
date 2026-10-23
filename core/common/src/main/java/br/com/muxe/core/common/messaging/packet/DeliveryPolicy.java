package br.com.muxe.core.common.messaging.packet;

import java.time.Duration;
import java.util.Objects;

public record DeliveryPolicy(IdempotencyLevel idempotency, Duration deduplicationWindow, Duration maxAge) {

    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(2);
    private static final Duration DEFAULT_MAX_AGE = Duration.ofSeconds(30);

    public DeliveryPolicy {
        Objects.requireNonNull(idempotency, "idempotency");
        Objects.requireNonNull(deduplicationWindow, "deduplicationWindow");
        Objects.requireNonNull(maxAge, "maxAge");
        if (deduplicationWindow.isNegative() || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("Delivery windows must be positive");
        }
    }

    public static DeliveryPolicy standard() {
        return new DeliveryPolicy(IdempotencyLevel.LOCAL, DEFAULT_WINDOW, DEFAULT_MAX_AGE);
    }

    public static DeliveryPolicy volatileState(Duration maxAge) {
        return new DeliveryPolicy(IdempotencyLevel.NONE, Duration.ZERO, maxAge);
    }

    public static DeliveryPolicy durable(Duration window) {
        return new DeliveryPolicy(IdempotencyLevel.DURABLE, window, DEFAULT_MAX_AGE);
    }

    public static DeliveryPolicy exclusive(Duration window) {
        return new DeliveryPolicy(IdempotencyLevel.EXCLUSIVE, window, DEFAULT_MAX_AGE);
    }

    public DeliveryPolicy withMaxAge(Duration value) {
        return new DeliveryPolicy(idempotency, deduplicationWindow, value);
    }
}
