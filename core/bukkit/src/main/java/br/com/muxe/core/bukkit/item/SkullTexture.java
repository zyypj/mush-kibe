package br.com.muxe.core.bukkit.item;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SkullTexture {

    public static final String TEXTURE_URL = "http://textures.minecraft.net/texture/";
    private static final Pattern HASH = Pattern.compile("[0-9a-fA-F]{32,}");

    private SkullTexture() {
    }

    public static String value(String texture) {
        if (texture == null || texture.isBlank()) {
            return null;
        }
        String trimmed = texture.trim();
        if (HASH.matcher(trimmed).matches()) {
            return encode(TEXTURE_URL + trimmed.toLowerCase(Locale.ROOT));
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return encode(trimmed);
        }
        return trimmed;
    }

    private static String encode(String url) {
        JsonObject skin = new JsonObject();
        skin.addProperty("url", url);
        JsonObject textures = new JsonObject();
        textures.add("SKIN", skin);
        JsonObject root = new JsonObject();
        root.add("textures", textures);
        return Base64.getEncoder().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));
    }
}
