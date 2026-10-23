package br.com.muxe.core.common.messaging.envelope;

public final class EnvelopeRejectedException extends RuntimeException {

    private final RejectionReason reason;

    public EnvelopeRejectedException(RejectionReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public EnvelopeRejectedException(RejectionReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public RejectionReason reason() {
        return reason;
    }
}
