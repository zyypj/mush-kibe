package br.com.muxe.core.proxy.editor.api;

import br.com.muxe.core.common.permission.editor.EditorSessions;
import br.com.muxe.core.proxy.editor.security.Authentication;
import br.com.muxe.core.proxy.editor.security.EditorAuthenticator;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EditorApi {

    private static final Pattern USER = Pattern.compile("/api/users/([0-9a-fA-F-]{36})");
    private static final Pattern MEMBERS = Pattern.compile("/api/groups/([a-z0-9_+-]{1,32})/members");

    private final EditorAuthenticator authenticator;
    private final EditorSessions sessions;
    private final ReadEndpoints reads;
    private final SaveEndpoint save;
    private final Logger logger;

    public EditorApi(EditorAuthenticator authenticator, EditorSessions sessions, ReadEndpoints reads, SaveEndpoint save, Logger logger) {
        this.authenticator = authenticator;
        this.sessions = sessions;
        this.reads = reads;
        this.save = save;
        this.logger = logger;
    }

    public ApiResponse handle(ApiRequest request) {
        try {
            return route(request);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.error(400, ApiResponse.INVALID_REQUEST, exception.getMessage());
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof IllegalArgumentException invalid) {
                return ApiResponse.error(400, ApiResponse.INVALID_REQUEST, invalid.getMessage());
            }
            return unavailable(request, exception);
        } catch (RuntimeException exception) {
            return unavailable(request, exception);
        }
    }

    private ApiResponse route(ApiRequest request) {
        Authentication authentication = authenticator.authenticate(request.authorization());
        if (authentication instanceof Authentication.Denied denied) {
            return ApiResponse.error(denied.status(), denied.code(), denied.message());
        }
        Authentication.Granted granted = (Authentication.Granted) authentication;
        String path = request.path();
        boolean get = request.method().equals("GET");
        boolean post = request.method().equals("POST");
        Matcher user = USER.matcher(path);
        Matcher members = MEMBERS.matcher(path);
        return switch (path) {
            case "/api/session" -> get ? reads.session(granted) : notAllowed();
            case "/api/data" -> get ? reads.data() : notAllowed();
            case "/api/users/search" -> get ? reads.search(request) : notAllowed();
            case "/api/actions" -> get ? reads.actions(request) : notAllowed();
            case "/api/changes" -> post ? save.save(granted, request) : notAllowed();
            case "/api/logout" -> post ? logout(granted) : notAllowed();
            default -> {
                if (user.matches()) {
                    yield get ? reads.user(user.group(1)) : notAllowed();
                }
                if (members.matches()) {
                    yield get ? reads.members(members.group(1)) : notAllowed();
                }
                yield ApiResponse.error(404, ApiResponse.NOT_FOUND, "Unknown endpoint");
            }
        };
    }

    private ApiResponse logout(Authentication.Granted granted) {
        sessions.close(granted.token());
        logger.info("Permission editor: " + granted.session().actorName() + " logged out");
        JsonObject json = new JsonObject();
        json.addProperty("closed", true);
        return ApiResponse.ok(json);
    }

    private static ApiResponse notAllowed() {
        return ApiResponse.error(405, ApiResponse.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    private ApiResponse unavailable(ApiRequest request, RuntimeException exception) {
        logger.log(Level.WARNING, "Permission editor could not answer " + request, exception);
        return ApiResponse.error(503, ApiResponse.UNAVAILABLE, "The server could not complete the request, try again");
    }
}
