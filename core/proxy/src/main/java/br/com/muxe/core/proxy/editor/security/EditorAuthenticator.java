package br.com.muxe.core.proxy.editor.security;

import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.permission.application.query.FindPermissionUserQuery;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionResolver;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.editor.EditorSession;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

public final class EditorAuthenticator {

    public static final String UNAUTHENTICATED = "editor.unauthenticated";
    public static final String SESSION_EXPIRED = "editor.session-expired";
    public static final String FORBIDDEN = "editor.forbidden";
    public static final String NOT_READY = "editor.not-ready";

    private static final String BEARER = "Bearer ";

    private final EditorSessions sessions;
    private final QueryBus queries;
    private final Supplier<PermissionData> data;
    private final Clock clock;

    public EditorAuthenticator(EditorSessions sessions, QueryBus queries, Supplier<PermissionData> data, Clock clock) {
        this.sessions = sessions;
        this.queries = queries;
        this.data = data;
        this.clock = clock;
    }

    public Authentication authenticate(Optional<String> authorization) {
        Optional<String> token = authorization
            .filter(header -> header.regionMatches(true, 0, BEARER, 0, BEARER.length()))
            .map(header -> header.substring(BEARER.length()).trim())
            .filter(value -> !value.isEmpty());
        if (token.isEmpty()) {
            return new Authentication.Denied(401, UNAUTHENTICATED, "Missing bearer token");
        }
        Optional<EditorSession> session = sessions.find(token.get());
        if (session.isEmpty()) {
            return new Authentication.Denied(401, SESSION_EXPIRED, "The editor session expired or does not exist");
        }
        PermissionData loaded = data.get();
        if (loaded.groups().isEmpty()) {
            return new Authentication.Denied(503, NOT_READY, "The groups are still loading, try again in a few seconds");
        }
        Instant now = clock.instant();
        PermissionUser actor = queries.ask(new FindPermissionUserQuery(session.get().actorId())).join();
        boolean allowed = PermissionResolver.resolve(loaded, actor, Contexts.NONE, now).has(EditorSessions.PERMISSION);
        if (!allowed) {
            sessions.close(token.get());
            return new Authentication.Denied(403, FORBIDDEN, "The session owner lost the editor permission");
        }
        return new Authentication.Granted(token.get(), session.get(), session.get().createdAt().plus(sessions.duration()));
    }
}
