package br.com.muxe.core.common.messaging.envelope;

import java.util.Locale;

public enum RejectionReason {
    TOO_LARGE,
    MALFORMED,
    BAD_MAGIC,
    UNSUPPORTED_PROTOCOL,
    BAD_SIGNATURE,
    UNKNOWN_TYPE,
    EXPIRED,
    MALFORMED_PAYLOAD,
    LATE_RESPONSE,
    IDEMPOTENCY_UNAVAILABLE;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }
}
