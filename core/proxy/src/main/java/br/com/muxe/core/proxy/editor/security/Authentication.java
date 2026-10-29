package br.com.muxe.core.proxy.editor.security;

import br.com.muxe.core.common.permission.editor.EditorSession;
import java.time.Instant;

public sealed interface Authentication {

    record Granted(String token, EditorSession session, Instant expiresAt) implements Authentication {

        @Override
        public String toString() {
            return "Granted[actor=" + session.actorName() + ", expiresAt=" + expiresAt + "]";
        }
    }

    record Denied(int status, String code, String message) implements Authentication {
    }
}
