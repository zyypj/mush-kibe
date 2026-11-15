package br.com.muxe.minigame.bedwars.lobby;

import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.scoreboard.SidebarContent;
import br.com.muxe.core.bukkit.stats.LevelBadges;
import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import br.com.muxe.minigame.bedwars.lobby.format.XpBar;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyScoreboardSettings;
import br.com.muxe.minigame.bedwars.stats.domain.PlayerStats;
import br.com.muxe.minigame.bedwars.stats.domain.StatCounters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class LobbySidebar {

    private LobbySidebar() {
    }

    public static SidebarContent content(LobbyScoreboardSettings settings, PlayerStats stats, LevelBadges badges, Collection<String> modes,
                                         Function<String, String> modeNames, int players) {
        BedWarsLevels.Progress progress = BedWarsLevels.progress(Math.max(0, stats.general().xp()));
        List<String> lines = new ArrayList<>();
        for (String line : settings.lines()) {
            if (line.trim().equals(LobbyScoreboardSettings.MODES)) {
                for (String mode : mostPlayed(settings, stats, modes)) {
                    StatCounters counters = stats.of(mode);
                    settings.modeLines().forEach(modeLine -> lines.add(LobbyLines.counters(modeLine, counters).replace("{mode}", modeNames.apply(mode))));
                }
                continue;
            }
            lines.add(line
                .replace("{level}", badges.of(progress.level()))
                .replace("{bar}", XpBar.of(progress, settings.barSegments()))
                .replace("{players}", CreditFormat.format(players)));
        }
        return new SidebarContent(settings.title(), lines);
    }

    public static List<String> mostPlayed(LobbyScoreboardSettings settings, PlayerStats stats, Collection<String> modes) {
        List<String> chosen = new ArrayList<>(stats.byMode().entrySet().stream()
            .filter(entry -> modes.contains(entry.getKey()) && entry.getValue().games() > 0)
            .sorted(Comparator.comparingLong((Map.Entry<String, StatCounters> entry) -> entry.getValue().games()).reversed()
                .thenComparingInt(entry -> order(settings, entry.getKey()))
                .thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey)
            .limit(settings.shownModes())
            .toList());
        for (String fallback : settings.defaultModes()) {
            if (chosen.size() >= settings.shownModes()) {
                break;
            }
            if (modes.contains(fallback) && !chosen.contains(fallback)) {
                chosen.add(fallback);
            }
        }
        return chosen;
    }

    private static int order(LobbyScoreboardSettings settings, String mode) {
        int index = settings.defaultModes().indexOf(mode);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
