package br.com.muxe.minigame.engine.match;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;

public final class CountdownTitles {

    private final Map<Integer, String> titles;

    public CountdownTitles(Map<Integer, String> titles) {
        this.titles = Map.copyOf(titles);
    }

    public static CountdownTitles from(ConfigurationSection section) {
        Map<Integer, String> titles = new TreeMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    titles.put(Integer.parseInt(key), section.getString(key, ""));
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Countdown title keys must be seconds, got: " + key, exception);
                }
            }
        }
        return new CountdownTitles(titles);
    }

    public Optional<String> at(int seconds) {
        return Optional.ofNullable(titles.get(seconds));
    }
}
