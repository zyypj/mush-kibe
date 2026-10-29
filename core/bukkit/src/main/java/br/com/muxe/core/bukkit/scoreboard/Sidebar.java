package br.com.muxe.core.bukkit.scoreboard;

import br.com.muxe.core.bukkit.text.Colors;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class Sidebar {

    public static final int MAX_LINES = 15;
    public static final int MAX_TITLE = 32;

    private static final String OBJECTIVE = "muxe";
    private static final String TEAM_PREFIX = "muxe-line-";

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> shown = new ArrayList<>();
    private String title = "";

    private Sidebar(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
        this.objective = scoreboard.registerNewObjective(OBJECTIVE, "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public static Sidebar create(Player player) {
        Sidebar sidebar = new Sidebar(Bukkit.getScoreboardManager().getNewScoreboard());
        player.setScoreboard(sidebar.scoreboard);
        return sidebar;
    }

    public Scoreboard scoreboard() {
        return scoreboard;
    }

    public void title(String value) {
        String translated = Colors.translate(value);
        String trimmed = translated.length() > MAX_TITLE ? translated.substring(0, MAX_TITLE) : translated;
        if (!trimmed.equals(title)) {
            title = trimmed;
            objective.setDisplayName(trimmed);
        }
    }

    public void lines(List<String> values) {
        List<String> lines = values.stream().limit(MAX_LINES).map(Colors::translate).toList();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Team team = team(index);
            if (index >= shown.size() || !shown.get(index).equals(line)) {
                SidebarText text = SidebarText.split(line);
                team.setPrefix(text.prefix());
                team.setSuffix(text.suffix());
            }
            objective.getScore(entry(index)).setScore(lines.size() - index);
        }
        for (int index = lines.size(); index < shown.size(); index++) {
            scoreboard.resetScores(entry(index));
            Team team = scoreboard.getTeam(TEAM_PREFIX + index);
            if (team != null) {
                team.unregister();
            }
        }
        shown.clear();
        shown.addAll(lines);
    }

    private Team team(int index) {
        Team team = scoreboard.getTeam(TEAM_PREFIX + index);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_PREFIX + index);
            team.addEntry(entry(index));
        }
        return team;
    }

    private static String entry(int index) {
        return ChatColor.values()[index].toString() + ChatColor.RESET;
    }
}
