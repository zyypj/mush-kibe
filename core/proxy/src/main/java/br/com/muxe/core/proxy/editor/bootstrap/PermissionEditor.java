package br.com.muxe.core.proxy.editor.bootstrap;

import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import br.com.muxe.core.common.store.ExpiringStore;
import br.com.muxe.core.proxy.config.EditorSettings;
import br.com.muxe.core.proxy.editor.api.EditorApi;
import br.com.muxe.core.proxy.editor.api.ReadEndpoints;
import br.com.muxe.core.proxy.editor.api.SaveEndpoint;
import br.com.muxe.core.proxy.editor.http.EditorHttpServer;
import br.com.muxe.core.proxy.editor.http.StaticAssets;
import br.com.muxe.core.proxy.editor.security.EditorAuthenticator;
import br.com.muxe.core.proxy.editor.security.RateLimiter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.logging.Logger;

public final class PermissionEditor {

    private PermissionEditor() {
    }

    public static EditorApi api(EditorSessions sessions, QueryBus queries, PermissionSync permissions, Clock clock, Logger logger) {
        EditorAuthenticator authenticator = new EditorAuthenticator(sessions, queries, permissions::data, clock);
        return new EditorApi(
            authenticator,
            sessions,
            new ReadEndpoints(queries, permissions, clock),
            new SaveEndpoint(permissions, clock, logger),
            logger
        );
    }

    public static EditorHttpServer start(EditorSettings settings, ExpiringStore store, QueryBus queries, PermissionSync permissions, Clock clock, Logger logger) throws IOException {
        EditorSessions sessions = new EditorSessions(store, clock, EditorSessions.DEFAULT_DURATION);
        EditorHttpServer.Options options = new EditorHttpServer.Options(
            api(sessions, queries, permissions, clock, logger),
            StaticAssets.load(PermissionEditor.class.getClassLoader()),
            new RateLimiter(settings.requestsPerMinute(), Duration.ofMinutes(1), clock),
            new RateLimiter(settings.failedLoginsPerTenMinutes(), Duration.ofMinutes(10), clock),
            settings.maxBodyBytes(),
            settings.publicUrl(),
            logger
        );
        return EditorHttpServer.start(new InetSocketAddress(settings.bind(), settings.port()), options);
    }
}
