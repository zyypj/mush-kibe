package br.com.muxe.core.proxy.editor.api;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ApiRequest(String method, String path, Map<String, String> query, Optional<String> authorization, String body) {

    public ApiRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        query = Map.copyOf(query);
        Objects.requireNonNull(authorization, "authorization");
        Objects.requireNonNull(body, "body");
    }

    public static ApiRequest get(String path, Optional<String> authorization) {
        int separator = path.indexOf('?');
        if (separator < 0) {
            return new ApiRequest("GET", path, Map.of(), authorization, "");
        }
        return new ApiRequest("GET", path.substring(0, separator), parseQuery(path.substring(separator + 1)), authorization, "");
    }

    public static ApiRequest post(String path, Optional<String> authorization, String body) {
        return new ApiRequest("POST", path, Map.of(), authorization, body);
    }

    public static Map<String, String> parseQuery(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return values;
        }
        for (String pair : raw.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            try {
                values.putIfAbsent(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                values.putIfAbsent(key, "");
            }
        }
        return values;
    }

    public Optional<String> parameter(String name) {
        return Optional.ofNullable(query.get(name));
    }

    @Override
    public String toString() {
        return "ApiRequest[" + method + " " + path + "]";
    }
}
