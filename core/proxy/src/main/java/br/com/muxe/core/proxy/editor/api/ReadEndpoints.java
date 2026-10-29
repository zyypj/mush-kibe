package br.com.muxe.core.proxy.editor.api;

import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.account.domain.Usernames;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.permission.application.query.FindPermissionUserQuery;
import br.com.muxe.core.common.permission.application.query.FindUsersWithNodeQuery;
import br.com.muxe.core.common.permission.application.query.RecentPermissionActionsQuery;
import br.com.muxe.core.common.permission.domain.PermissionIds;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import br.com.muxe.core.proxy.editor.json.PermissionJson;
import br.com.muxe.core.proxy.editor.security.Authentication;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ReadEndpoints {

    public static final String USER_NOT_FOUND = "editor.user-not-found";
    public static final String GROUP_NOT_FOUND = "editor.group-not-found";
    public static final int MAX_MEMBERS = 200;
    public static final int DEFAULT_ACTIONS = 100;

    private final QueryBus queries;
    private final PermissionSync permissions;
    private final Clock clock;

    public ReadEndpoints(QueryBus queries, PermissionSync permissions, Clock clock) {
        this.queries = queries;
        this.permissions = permissions;
        this.clock = clock;
    }

    public ApiResponse session(Authentication.Granted granted) {
        JsonObject json = new JsonObject();
        json.addProperty("actor", granted.session().actorName());
        json.addProperty("actorId", granted.session().actorId().toString());
        json.addProperty("createdAt", granted.session().createdAt().toEpochMilli());
        json.addProperty("expiresAt", granted.expiresAt().toEpochMilli());
        json.addProperty("now", clock.instant().toEpochMilli());
        return ApiResponse.ok(json);
    }

    public ApiResponse data() {
        return ApiResponse.ok(PermissionJson.data(permissions.data()));
    }

    public ApiResponse search(ApiRequest request) {
        String name = request.parameter("name").map(String::trim).orElse("");
        Optional<UUID> id = uuid(name);
        if (id.isEmpty() && !Usernames.isValid(name)) {
            return ApiResponse.error(400, ApiResponse.INVALID_REQUEST, "Search by a valid player name or UUID");
        }
        FindAccountDetailsQuery query = id.map(FindAccountDetailsQuery::byId).orElseGet(() -> FindAccountDetailsQuery.byUsername(name));
        return user(queries.ask(query).join());
    }

    public ApiResponse user(String rawId) {
        Optional<UUID> id = uuid(rawId);
        if (id.isEmpty()) {
            return ApiResponse.error(400, ApiResponse.INVALID_REQUEST, "Invalid player id");
        }
        return user(queries.ask(FindAccountDetailsQuery.byId(id.get())).join());
    }

    public ApiResponse members(String groupId) {
        if (!PermissionIds.isValid(groupId) || permissions.data().group(groupId).isEmpty()) {
            return ApiResponse.error(404, GROUP_NOT_FOUND, "Unknown group " + groupId);
        }
        List<UUID> ids = queries.ask(new FindUsersWithNodeQuery(PermissionNode.GROUP_PREFIX + groupId)).join();
        JsonArray members = new JsonArray();
        ids.stream().limit(MAX_MEMBERS).forEach(id -> {
            JsonObject member = new JsonObject();
            member.addProperty("id", id.toString());
            queries.ask(FindAccountDetailsQuery.byId(id)).join()
                .flatMap(details -> details.account().currentUsername())
                .ifPresent(username -> member.addProperty("username", username));
            members.add(member);
        });
        JsonObject json = new JsonObject();
        json.addProperty("group", groupId);
        json.addProperty("total", ids.size());
        json.add("members", members);
        return ApiResponse.ok(json);
    }

    public ApiResponse actions(ApiRequest request) {
        int limit;
        try {
            limit = request.parameter("limit").map(Integer::parseInt).orElse(DEFAULT_ACTIONS);
        } catch (NumberFormatException exception) {
            return ApiResponse.error(400, ApiResponse.INVALID_REQUEST, "limit must be a number");
        }
        RecentPermissionActionsQuery query = new RecentPermissionActionsQuery(Math.clamp(limit, 1, 500));
        return ApiResponse.ok(PermissionJson.array(queries.ask(query).join().stream().map(PermissionJson::action).toList()));
    }

    private ApiResponse user(Optional<AccountDetails> details) {
        if (details.isEmpty()) {
            return ApiResponse.error(404, USER_NOT_FOUND, "No player with that name or id");
        }
        PermissionUser user = queries.ask(new FindPermissionUserQuery(details.get().account().id())).join();
        return ApiResponse.ok(PermissionJson.user(details.get().account(), user));
    }

    private static Optional<UUID> uuid(String raw) {
        if (raw == null || raw.length() != 36) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
