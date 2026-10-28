package br.com.muxe.core.proxy.editor.json;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class PermissionJson {

    private PermissionJson() {
    }

    public static JsonObject data(PermissionData data) {
        JsonObject json = new JsonObject();
        json.addProperty("defaultGroup", Group.DEFAULT);
        json.add("groups", array(data.groupsByWeight().stream().map(PermissionJson::group).toList()));
        json.add("tags", array(data.tagsByWeight().stream().map(PermissionJson::tag).toList()));
        json.add("tracks", array(data.tracks().values().stream().sorted(Comparator.comparing(Track::id)).map(PermissionJson::track).toList()));
        return json;
    }

    public static JsonObject group(Group group) {
        JsonObject json = new JsonObject();
        json.addProperty("id", group.id());
        json.addProperty("displayName", group.displayName());
        json.addProperty("weight", group.weight());
        json.add("tag", optional(group.tag()));
        json.add("nodes", nodes(group.nodes()));
        return json;
    }

    public static JsonObject tag(Tag tag) {
        JsonObject json = new JsonObject();
        json.addProperty("id", tag.id());
        json.addProperty("name", tag.name());
        json.addProperty("color", tag.color());
        json.addProperty("weight", tag.weight());
        json.add("prefix", optional(tag.prefix()));
        json.addProperty("decorative", tag.decorative());
        json.addProperty("display", tag.display());
        json.addProperty("chatPrefix", tag.chatPrefix());
        return json;
    }

    public static JsonObject track(Track track) {
        JsonObject json = new JsonObject();
        json.addProperty("id", track.id());
        JsonArray groups = new JsonArray();
        track.groups().forEach(groups::add);
        json.add("groups", groups);
        return json;
    }

    public static JsonObject user(Account account, PermissionUser user) {
        JsonObject json = new JsonObject();
        json.addProperty("id", user.id().toString());
        json.add("username", optional(account.currentUsername()));
        json.addProperty("premium", account.premium());
        json.addProperty("firstSeenAt", account.firstSeenAt().toEpochMilli());
        json.addProperty("lastSeenAt", account.lastSeenAt().toEpochMilli());
        json.add("nodes", nodes(user.nodes()));
        json.add("selectedTag", optional(user.selectedTag()));
        return json;
    }

    public static JsonObject action(PermissionAction action) {
        JsonObject json = new JsonObject();
        json.addProperty("at", action.at().toEpochMilli());
        json.addProperty("actor", action.actor());
        json.addProperty("description", action.description());
        return json;
    }

    public static JsonObject node(PermissionNode node) {
        JsonObject json = new JsonObject();
        json.addProperty("key", node.key());
        json.addProperty("value", node.value());
        JsonObject contexts = new JsonObject();
        node.contexts().values().forEach(contexts::addProperty);
        json.add("contexts", contexts);
        json.add("expiresAt", node.expiresAt().<JsonElement>map(PermissionJson::millis).orElse(JsonNull.INSTANCE));
        json.addProperty("addedAt", node.addedAt().toEpochMilli());
        json.addProperty("updatedAt", node.updatedAt().toEpochMilli());
        return json;
    }

    public static JsonArray array(List<? extends JsonElement> elements) {
        JsonArray array = new JsonArray();
        elements.forEach(array::add);
        return array;
    }

    private static JsonArray nodes(List<PermissionNode> nodes) {
        return array(nodes.stream()
            .sorted(Comparator.comparing(PermissionNode::key).thenComparing(node -> node.contexts().encode()))
            .map(PermissionJson::node)
            .toList());
    }

    private static JsonPrimitive millis(Instant instant) {
        return new JsonPrimitive(instant.toEpochMilli());
    }

    private static JsonElement optional(Optional<String> value) {
        return value.<JsonElement>map(JsonPrimitive::new).orElse(JsonNull.INSTANCE);
    }
}
