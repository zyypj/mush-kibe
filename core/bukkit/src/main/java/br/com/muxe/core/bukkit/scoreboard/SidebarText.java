package br.com.muxe.core.bukkit.scoreboard;

import org.bukkit.ChatColor;

public record SidebarText(String prefix, String suffix) {

    public static final int MAX_PART = 16;

    public static SidebarText split(String line) {
        if (line.length() <= MAX_PART) {
            return new SidebarText(line, "");
        }
        int cut = MAX_PART;
        if (line.charAt(cut - 1) == ChatColor.COLOR_CHAR) {
            cut--;
        }
        String prefix = line.substring(0, cut);
        String remainder = line.substring(cut);
        String carried = startsWithColor(remainder) ? "" : ChatColor.getLastColors(prefix);
        return new SidebarText(prefix, trim(carried + remainder));
    }

    private static boolean startsWithColor(String text) {
        if (text.length() < 2 || text.charAt(0) != ChatColor.COLOR_CHAR) {
            return false;
        }
        ChatColor color = ChatColor.getByChar(text.charAt(1));
        return color != null && color.isColor();
    }

    private static String trim(String text) {
        String trimmed = text.length() > MAX_PART ? text.substring(0, MAX_PART) : text;
        while (!trimmed.isEmpty() && trimmed.charAt(trimmed.length() - 1) == ChatColor.COLOR_CHAR) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
