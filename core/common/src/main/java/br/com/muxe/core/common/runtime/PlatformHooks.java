package br.com.muxe.core.common.runtime;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.server.heartbeat.ServerLoad;
import java.util.Objects;
import java.util.logging.Logger;

public record PlatformHooks(Logger logger, MainThreadGuard mainThreadGuard, ServerLoad serverLoad) {

    public PlatformHooks {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(mainThreadGuard, "mainThreadGuard");
        Objects.requireNonNull(serverLoad, "serverLoad");
    }
}
