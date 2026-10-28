package br.com.muxe.core.proxy.editor.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StaticAssets {

    public static final String ROOT = "editor/";
    public static final List<String> FILES = List.of(
        "index.html",
        "style.css",
        "app.js",
        "api.js",
        "state.js",
        "ui.js",
        "colors.js",
        "nodes.js",
        "groups.js",
        "tags.js",
        "tracks.js",
        "users.js",
        "history.js",
        "favicon.png",
        "logo.png"
    );

    private final Map<String, Asset> assets;

    private StaticAssets(Map<String, Asset> assets) {
        this.assets = Map.copyOf(assets);
    }

    public static StaticAssets load(ClassLoader loader) {
        Map<String, Asset> assets = new HashMap<>();
        for (String file : FILES) {
            try (InputStream in = loader.getResourceAsStream(ROOT + file)) {
                if (in == null) {
                    throw new IllegalStateException("The editor file " + file + " is missing from the plugin jar");
                }
                assets.put("/" + file, new Asset(in.readAllBytes(), contentType(file)));
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        assets.put("/", assets.get("/index.html"));
        return new StaticAssets(assets);
    }

    public Optional<Asset> find(String path) {
        return Optional.ofNullable(assets.get(path));
    }

    static String contentType(String file) {
        if (file.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (file.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (file.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (file.endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }

    public record Asset(byte[] bytes, String contentType) {
    }
}
