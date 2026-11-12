package br.com.muxe.core.bukkit.scheduledgame;

import br.com.muxe.core.common.schedule.WeeklySchedule;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record ScheduledGame(String id, String game, String mode, String command, String lobbyGroup, String announcement, WeeklySchedule schedule,
                            Duration announceEvery) {

    public ScheduledGame {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(lobbyGroup, "lobbyGroup");
        Objects.requireNonNull(announcement, "announcement");
        Objects.requireNonNull(schedule, "schedule");
        if (announceEvery.isNegative() || announceEvery.isZero()) {
            throw new IllegalArgumentException("A scheduled game announces at a positive interval, got: " + announceEvery);
        }
    }

    public static Map<String, ScheduledGame> from(ConfigurationSection section) {
        Map<String, ScheduledGame> games = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = Objects.requireNonNull(section.getConfigurationSection(id), "scheduled-games." + id);
            WeeklySchedule schedule = new WeeklySchedule(ZoneId.of(entry.getString("time-zone", "America/Sao_Paulo")), times(entry, "weekdays"),
                times(entry, "weekends"), Duration.ofMinutes(entry.getLong("window-minutes", 15)));
            games.put(id, new ScheduledGame(id, entry.getString("game", ""), entry.getString("mode", id), entry.getString("command", id),
                entry.getString("lobby-group", "lobby"), entry.getString("announcement", id + "-announce"), schedule,
                Duration.ofSeconds(entry.getLong("announce-every-seconds", 60))));
        }
        return games;
    }

    private static List<LocalTime> times(ConfigurationSection entry, String path) {
        return entry.getStringList(path).stream().map(LocalTime::parse).toList();
    }
}
