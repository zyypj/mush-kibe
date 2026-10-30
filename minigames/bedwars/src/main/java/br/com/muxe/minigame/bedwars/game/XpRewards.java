package br.com.muxe.minigame.bedwars.game;

import org.bukkit.configuration.ConfigurationSection;

public record XpRewards(int perMinute, int perTeammate, int gameWin, int bedDestroyed, int regularKill, int finalKill) {

    public static XpRewards from(ConfigurationSection section) {
        return new XpRewards(
            section.getInt("per-minute", 10),
            section.getInt("per-teammate", 5),
            section.getInt("game-win", 250),
            section.getInt("bed-destroyed", 100),
            section.getInt("regular-kill", 12),
            section.getInt("final-kill", 60)
        );
    }

    public int endOfMatch(int minutesPlayed, int teamSize, boolean won) {
        return perMinute * Math.max(0, minutesPlayed) + perTeammate * Math.max(0, teamSize) + (won ? gameWin : 0);
    }
}
