package br.com.muxe.minigame.bedwars.lobby.format;

import br.com.muxe.core.common.stats.domain.BedWarsLevels;


public final class XpBar {

    private static final String SEGMENT = "\u25ae";

    private XpBar() {
    }

    public static String of(BedWarsLevels.Progress progress, int segments) {
        int filled = progress.needed() <= 0 ? segments : (int) Math.min(segments, progress.xp() * segments / progress.needed());
        return "&f[&a" + SEGMENT.repeat(filled) + "&7" + SEGMENT.repeat(segments - filled) + "&f] &7(" + compact(progress.xp()) + "/" + compact(progress.needed()) + ")";
    }

    public static String compact(long value) {
        if (value < 1000) {
            return Long.toString(Math.max(0, value));
        }
        long tenths = value / 100;
        long whole = tenths / 10;
        long decimal = tenths % 10;
        return decimal == 0 ? whole + "k" : whole + "." + decimal + "k";
    }
}
