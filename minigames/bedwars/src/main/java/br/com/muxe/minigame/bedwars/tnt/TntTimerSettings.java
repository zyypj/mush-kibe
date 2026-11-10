package br.com.muxe.minigame.bedwars.tnt;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;

public record TntTimerSettings(String format, String decimalSeparator, int decimals, double height, double viewDistance, long updateTicks, double green, double yellow, double orange,
                               String greenColor, String yellowColor, String orangeColor, String redColor) {

    public static TntTimerSettings from(ConfigurationSection section) {
        return new TntTimerSettings(
            section.getString("format", "{color}{seconds}s"),
            section.getString("decimal-separator", ","),
            Math.max(0, section.getInt("decimals", 2)),
            section.getDouble("height", 1.1),
            Math.max(1, section.getDouble("view-distance", 24)),
            Math.max(1, section.getLong("update-ticks", 1)),
            section.getDouble("green-threshold", 0.75),
            section.getDouble("yellow-threshold", 0.5),
            section.getDouble("orange-threshold", 0.25),
            section.getString("colors.green", "&a"),
            section.getString("colors.yellow", "&e"),
            section.getString("colors.orange", "&6"),
            section.getString("colors.red", "&c")
        );
    }

    public String text(int remainingTicks, int fuseTicks) {
        double ratio = Math.clamp(remainingTicks / (double) Math.max(1, fuseTicks), 0, 1);
        String color = ratio >= green ? greenColor : ratio >= yellow ? yellowColor : ratio >= orange ? orangeColor : redColor;
        String seconds = String.format(Locale.US, "%." + decimals + "f", Math.max(0, remainingTicks) / 20.0).replace(".", decimalSeparator);
        return format.replace("{color}", color).replace("{seconds}", seconds);
    }
}
