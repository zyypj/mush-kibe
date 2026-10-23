package br.com.muxe.core.common.messaging.packet;

public enum IdempotencyLevel {
    NONE,
    LOCAL,
    DURABLE,
    EXCLUSIVE
}
