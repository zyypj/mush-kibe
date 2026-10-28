package br.com.muxe.core.proxy.editor.api;

import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.proxy.editor.json.ChangeReader;
import br.com.muxe.core.proxy.editor.json.PermissionJson;
import br.com.muxe.core.proxy.editor.security.Authentication;
import com.google.gson.JsonObject;
import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SaveEndpoint {

    private static final int LOGGED_CHANGES = 20;

    private final PermissionSync permissions;
    private final Clock clock;
    private final Logger logger;

    public SaveEndpoint(PermissionSync permissions, Clock clock, Logger logger) {
        this.permissions = permissions;
        this.clock = clock;
        this.logger = logger;
    }

    public ApiResponse save(Authentication.Granted granted, ApiRequest request) {
        List<PermissionChange> changes = ChangeReader.read(request.body(), clock.instant());
        String actor = granted.session().actorName();
        Result<Void> result = permissions.apply(actor, changes).join();
        if (result instanceof Result.Failure<Void> failure) {
            return ApiResponse.error(400, failure.problem().code(), failure.problem().message());
        }
        logger.info("Permission editor: " + actor + " (" + granted.session().actorId() + ") saved " + changes.size() + " change(s): " + summary(changes));
        JsonObject json = new JsonObject();
        json.addProperty("saved", changes.size());
        json.add("data", PermissionJson.data(permissions.data()));
        return ApiResponse.ok(json);
    }

    private static String summary(List<PermissionChange> changes) {
        String listed = changes.stream().limit(LOGGED_CHANGES).map(PermissionChange::describe).collect(Collectors.joining("; "));
        return changes.size() > LOGGED_CHANGES ? listed + "; and " + (changes.size() - LOGGED_CHANGES) + " more" : listed;
    }
}
