package br.com.muxe.core.common.messaging.idempotency;

public enum ClaimResult {
    FIRST,
    DUPLICATE,
    UNAVAILABLE
}
