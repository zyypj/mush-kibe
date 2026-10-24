package br.com.muxe.core.bukkit.npc.skin;

import br.com.muxe.core.bukkit.npc.NpcSkin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class MojangSkins {

    private static final String PROFILE_BY_NAME = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final int OK = 200;

    private final HttpClient http;
    private final Duration timeout;

    public MojangSkins(HttpClient http, Duration timeout) {
        this.http = http;
        this.timeout = timeout;
    }

    public static MojangSkins standard() {
        Duration timeout = Duration.ofSeconds(5);
        return new MojangSkins(HttpClient.newBuilder().connectTimeout(timeout).build(), timeout);
    }

    public CompletableFuture<Optional<NpcSkin>> byName(String name) {
        return get(PROFILE_BY_NAME + URLEncoder.encode(name, StandardCharsets.UTF_8))
            .thenCompose(body -> body.flatMap(MojangSkins::parseId)
                .map(id -> get(SESSION_PROFILE + id + "?unsigned=false").thenApply(profile -> profile.flatMap(MojangSkins::parseTextures)))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    public static Optional<String> parseId(String json) {
        return object(json).map(root -> root.get("id")).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString);
    }

    public static Optional<NpcSkin> parseTextures(String json) {
        Optional<JsonObject> root = object(json);
        if (root.isEmpty() || !root.get().has("properties") || !root.get().get("properties").isJsonArray()) {
            return Optional.empty();
        }
        JsonArray properties = root.get().getAsJsonArray("properties");
        for (JsonElement element : properties) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            if (property.has("name") && "textures".equals(property.get("name").getAsString()) && property.has("value") && property.has("signature")) {
                return Optional.of(new NpcSkin(property.get("value").getAsString(), property.get("signature").getAsString()));
            }
        }
        return Optional.empty();
    }

    private CompletableFuture<Optional<String>> get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == OK ? Optional.of(response.body()) : Optional.<String>empty())
            .exceptionally(error -> Optional.empty());
    }

    @SuppressWarnings("deprecation")
    private static Optional<JsonObject> object(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonElement parsed = new JsonParser().parse(json);
            return parsed.isJsonObject() ? Optional.of(parsed.getAsJsonObject()) : Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
