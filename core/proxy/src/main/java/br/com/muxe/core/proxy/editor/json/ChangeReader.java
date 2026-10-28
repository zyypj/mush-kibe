package br.com.muxe.core.proxy.editor.json;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ChangeReader {

    public static final int MAX_CHANGES = 500;
    public static final int MAX_NODES = 5_000;
    public static final int MAX_CONTEXTS = 8;

    private ChangeReader() {
    }

    public static List<PermissionChange> read(String body, Instant now) {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("The body is not valid JSON");
        }
        JsonArray changes = array(object(root, "body").get("changes"), "changes");
        if (changes.isEmpty() || changes.size() > MAX_CHANGES) {
            throw new IllegalArgumentException("A save has 1 to " + MAX_CHANGES + " changes, got " + changes.size());
        }
        List<PermissionChange> parsed = new ArrayList<>();
        for (JsonElement element : changes) {
            parsed.add(change(object(element, "change"), now));
        }
        return parsed;
    }

    private static PermissionChange change(JsonObject json, Instant now) {
        String type = string(json, "type");
        return switch (type) {
            case "put-group" -> new PermissionChange.PutGroup(group(object(json.get("group"), "group"), now));
            case "delete-group" -> new PermissionChange.DeleteGroup(string(json, "id"));
            case "put-tag" -> new PermissionChange.PutTag(tag(object(json.get("tag"), "tag")));
            case "delete-tag" -> new PermissionChange.DeleteTag(string(json, "id"));
            case "put-track" -> new PermissionChange.PutTrack(track(object(json.get("track"), "track")));
            case "delete-track" -> new PermissionChange.DeleteTrack(string(json, "id"));
            case "put-user" -> new PermissionChange.PutUser(user(object(json.get("user"), "user"), now));
            default -> throw new IllegalArgumentException("Unknown change type: " + type);
        };
    }

    private static Group group(JsonObject json, Instant now) {
        return new Group(string(json, "id"), string(json, "displayName"), integer(json, "weight"), optionalString(json, "tag"), nodes(json, now));
    }

    private static Tag tag(JsonObject json) {
        return new Tag(string(json, "id"), string(json, "name"), string(json, "color"), integer(json, "weight"), optionalString(json, "prefix"), bool(json, "decorative", false));
    }

    private static Track track(JsonObject json) {
        List<String> groups = new ArrayList<>();
        for (JsonElement element : array(json.get("groups"), "groups")) {
            groups.add(primitiveString(element, "groups"));
        }
        return new Track(string(json, "id"), groups);
    }

    private static PermissionUser user(JsonObject json, Instant now) {
        UUID id;
        try {
            id = UUID.fromString(string(json, "id"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid user id");
        }
        return new PermissionUser(id, nodes(json, now), optionalString(json, "selectedTag"));
    }

    private static List<PermissionNode> nodes(JsonObject owner, Instant now) {
        JsonElement raw = owner.get("nodes");
        if (raw == null || raw.isJsonNull()) {
            return List.of();
        }
        JsonArray array = array(raw, "nodes");
        if (array.size() > MAX_NODES) {
            throw new IllegalArgumentException("At most " + MAX_NODES + " permissions per group or player");
        }
        List<PermissionNode> nodes = new ArrayList<>();
        for (JsonElement element : array) {
            PermissionNode node = node(object(element, "node"), now);
            if (nodes.stream().anyMatch(node::sameSlot)) {
                throw new IllegalArgumentException("The permission " + node.key() + " appears twice with the same contexts");
            }
            nodes.add(node);
        }
        return nodes;
    }

    private static PermissionNode node(JsonObject json, Instant now) {
        Instant added = optionalMillis(json, "addedAt").filter(instant -> !instant.isAfter(now)).orElse(now);
        Instant updated = optionalMillis(json, "updatedAt").filter(instant -> !instant.isAfter(now) && !instant.isBefore(added)).orElse(now);
        return new PermissionNode(string(json, "key"), bool(json, "value", true), contexts(json), optionalMillis(json, "expiresAt"), added, updated);
    }

    private static Contexts contexts(JsonObject json) {
        JsonElement raw = json.get("contexts");
        if (raw == null || raw.isJsonNull()) {
            return Contexts.NONE;
        }
        JsonObject object = object(raw, "contexts");
        if (object.size() > MAX_CONTEXTS) {
            throw new IllegalArgumentException("At most " + MAX_CONTEXTS + " contexts per permission");
        }
        Map<String, String> values = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> values.put(entry.getKey(), primitiveString(entry.getValue(), "contexts")));
        return new Contexts(values);
    }

    private static JsonObject object(JsonElement element, String name) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("'" + name + "' must be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonElement element, String name) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("'" + name + "' must be a list");
        }
        return element.getAsJsonArray();
    }

    private static String string(JsonObject json, String name) {
        return primitiveString(json.get(name), name);
    }

    private static String primitiveString(JsonElement element, String name) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'" + name + "' must be text");
        }
        String value = element.getAsString();
        if (value.length() > 256) {
            throw new IllegalArgumentException("'" + name + "' is too long");
        }
        return value;
    }

    private static Optional<String> optionalString(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(primitiveString(element, name));
    }

    private static int integer(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("'" + name + "' must be a number");
        }
        double value = element.getAsDouble();
        if (value != Math.rint(value) || value < -1_000_000 || value > 1_000_000) {
            throw new IllegalArgumentException("'" + name + "' must be a whole number between -1000000 and 1000000");
        }
        return (int) value;
    }

    private static boolean bool(JsonObject json, String name, boolean fallback) {
        JsonElement element = json.get(name);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("'" + name + "' must be true or false");
        }
        return element.getAsBoolean();
    }

    private static Optional<Instant> optionalMillis(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("'" + name + "' must be a timestamp in milliseconds");
        }
        double value = element.getAsDouble();
        if (value != Math.rint(value) || value < 0 || value > 32_503_680_000_000d) {
            throw new IllegalArgumentException("'" + name + "' is not a valid timestamp");
        }
        return Optional.of(Instant.ofEpochMilli((long) value));
    }
}
