package br.com.muxe.core.proxy.editor.http;

import java.util.Locale;

public final class OriginCheck {

    private OriginCheck() {
    }

    public static boolean allowed(String origin, String host, String publicUrl) {
        String normalized = origin.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(strip(publicUrl))) {
            return true;
        }
        if (host == null || host.isBlank()) {
            return false;
        }
        String self = host.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("http://" + self) || normalized.equals("https://" + self);
    }

    private static String strip(String url) {
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.endsWith("/") ? lower.substring(0, lower.length() - 1) : lower;
    }
}
