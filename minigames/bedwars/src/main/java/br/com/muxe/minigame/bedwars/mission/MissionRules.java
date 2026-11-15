package br.com.muxe.minigame.bedwars.mission;

import org.bukkit.configuration.ConfigurationSection;

public record MissionRules(int fastWinSeconds, int endStoneRadius, int endStoneBlocks, int bedbugAssistSeconds, int fewBlocksMax, int sharpnessLevel) {

    public static final MissionRules DEFAULT = new MissionRules(210, 3, 10, 10, 16, 2);

    public static MissionRules from(ConfigurationSection section) {
        if (section == null) {
            return DEFAULT;
        }
        return new MissionRules(
            section.getInt("fast-win-seconds", DEFAULT.fastWinSeconds()),
            section.getInt("end-stone-radius", DEFAULT.endStoneRadius()),
            section.getInt("end-stone-blocks", DEFAULT.endStoneBlocks()),
            section.getInt("bedbug-assist-seconds", DEFAULT.bedbugAssistSeconds()),
            section.getInt("few-blocks-max", DEFAULT.fewBlocksMax()),
            section.getInt("sharpness-level", DEFAULT.sharpnessLevel())
        );
    }
}
