package br.com.muxe.core.bukkit.text;

import org.bukkit.ChatColor;

public final class Colors {

    private Colors() {
    }

    public static String translate(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String strip(String text) {
        return text == null ? "" : ChatColor.stripColor(translate(text));
    }
}
