package br.com.muxe.core.common.messaging;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record MessagingConfig(String secret, List<String> previousSecrets, Duration requestTimeout, int maxEnvelopeBytes) {

    public MessagingConfig {
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        previousSecrets = List.copyOf(previousSecrets);
        if (maxEnvelopeBytes < 1024) {
            throw new IllegalArgumentException("maxEnvelopeBytes must be at least 1024");
        }
    }

    @Override
    public String toString() {
        return "MessagingConfig[requestTimeout=" + requestTimeout + ", maxEnvelopeBytes=" + maxEnvelopeBytes + "]";
    }
}
