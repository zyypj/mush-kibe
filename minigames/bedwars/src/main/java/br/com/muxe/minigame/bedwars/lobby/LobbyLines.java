package br.com.muxe.minigame.bedwars.lobby;

import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.stats.LevelBadges;
import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import br.com.muxe.minigame.bedwars.lobby.format.XpBar;
import br.com.muxe.minigame.bedwars.stats.domain.StatCounters;
import java.util.List;

public final class LobbyLines {

    private LobbyLines() {
    }

    public static List<String> npc(List<String> lines, int players, String blink) {
        return lines.stream().map(line -> line.replace("{players}", CreditFormat.format(players)).replace("{blink}", blink)).toList();
    }

    public static String blink(List<String> colors, long millis, long periodMillis) {
        if (colors.isEmpty()) {
            return "";
        }
        return colors.get((int) ((millis / periodMillis) % colors.size()));
    }

    public static List<String> stats(List<String> lines, StatCounters general, LevelBadges badges, int segments) {
        BedWarsLevels.Progress progress = BedWarsLevels.progress(Math.max(0, general.xp()));
        return lines.stream().map(line -> counters(line, general)
            .replace("{level}", badges.of(progress.level()))
            .replace("{bar}", XpBar.of(progress, segments))).toList();
    }

    public static String counters(String line, StatCounters counters) {
        return line
            .replace("{games}", CreditFormat.format(counters.games()))
            .replace("{beds-broken}", CreditFormat.format(counters.bedsBroken()))
            .replace("{beds-lost}", CreditFormat.format(counters.bedsLost()))
            .replace("{beds}", CreditFormat.format(counters.bedsBroken()))
            .replace("{kills}", CreditFormat.format(counters.kills()))
            .replace("{deaths}", CreditFormat.format(counters.deaths()))
            .replace("{assists}", CreditFormat.format(counters.assists()))
            .replace("{final-kills}", CreditFormat.format(counters.finalKills()))
            .replace("{final-deaths}", CreditFormat.format(counters.finalDeaths()))
            .replace("{final-assists}", CreditFormat.format(counters.finalAssists()))
            .replace("{wins}", CreditFormat.format(counters.wins()))
            .replace("{losses}", CreditFormat.format(counters.losses()))
            .replace("{best-winstreak}", CreditFormat.format(counters.bestWinstreak()))
            .replace("{winstreak}", CreditFormat.format(counters.winstreak()))
            .replace("{xp}", CreditFormat.format(counters.xp()));
    }
}
