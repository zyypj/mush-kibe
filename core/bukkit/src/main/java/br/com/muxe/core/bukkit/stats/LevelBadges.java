package br.com.muxe.core.bukkit.stats;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;

public final class LevelBadges {

    private final NavigableMap<Integer, String> badges;
    private final int rainbowFrom;
    private final String rainbowOpen;
    private final List<String> rainbowDigits;
    private final String rainbowClose;

    public LevelBadges(Map<Integer, String> badges, int rainbowFrom, String rainbowOpen, List<String> rainbowDigits, String rainbowClose) {
        if (badges.isEmpty() || rainbowDigits.isEmpty()) {
            throw new IllegalArgumentException("Level badges need at least one badge and one rainbow colour");
        }
        this.badges = new TreeMap<>(badges);
        this.rainbowFrom = rainbowFrom;
        this.rainbowOpen = rainbowOpen;
        this.rainbowDigits = List.copyOf(rainbowDigits);
        this.rainbowClose = rainbowClose;
    }

    public static LevelBadges from(ConfigurationSection section) {
        ConfigurationSection badges = Objects.requireNonNull(section.getConfigurationSection("badges"), "Missing configuration section " + section.getCurrentPath() + ".badges");
        Map<Integer, String> byLevel = new TreeMap<>();
        for (String key : badges.getKeys(false)) {
            byLevel.put(Integer.parseInt(key), badges.getString(key));
        }
        ConfigurationSection rainbow = Objects.requireNonNull(section.getConfigurationSection("rainbow"), "Missing configuration section " + section.getCurrentPath() + ".rainbow");
        return new LevelBadges(byLevel, rainbow.getInt("from", 1000), rainbow.getString("open", ""), rainbow.getStringList("digits"), rainbow.getString("close", ""));
    }

    public String of(int level) {
        String digits = Integer.toString(Math.max(0, level));
        if (level >= rainbowFrom) {
            StringBuilder badge = new StringBuilder(rainbowOpen);
            for (int index = 0; index < digits.length(); index++) {
                badge.append(rainbowDigits.get(index % rainbowDigits.size())).append(digits.charAt(index));
            }
            return badge.append(rainbowClose).toString();
        }
        Map.Entry<Integer, String> badge = badges.floorEntry(Math.max(0, level));
        return (badge == null ? badges.firstEntry() : badge).getValue().replace("{level}", digits);
    }
}
