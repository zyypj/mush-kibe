package br.com.muxe.core.proxy.routing;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import java.util.Optional;

public final class ConnectReplies {

    public static final String ALREADY_CONNECTED = "already-connected";
    public static final String ALREADY_CONNECTING = "already-connecting";
    public static final String CONNECT_FAILED = "connect-failed";

    private ConnectReplies() {
    }

    public static Optional<String> messageKey(Optional<Status> status, boolean quietWhenThere) {
        if (status.isEmpty()) {
            return Optional.of(CONNECT_FAILED);
        }
        return switch (status.get()) {
            case SUCCESS, CONNECTION_CANCELLED -> Optional.empty();
            case ALREADY_CONNECTED -> quietWhenThere ? Optional.empty() : Optional.of(ALREADY_CONNECTED);
            case CONNECTION_IN_PROGRESS -> Optional.of(ALREADY_CONNECTING);
            case SERVER_DISCONNECTED -> Optional.of(CONNECT_FAILED);
        };
    }
}
