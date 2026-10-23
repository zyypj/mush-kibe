package br.com.muxe.core.common.messaging.envelope;

public enum EnvelopeKind {
    MESSAGE(0),
    REQUEST(1),
    RESPONSE(2);

    private final int code;

    EnvelopeKind(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static EnvelopeKind fromCode(int code) {
        for (EnvelopeKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new EnvelopeRejectedException(RejectionReason.MALFORMED, "Unknown envelope kind: " + code);
    }
}
