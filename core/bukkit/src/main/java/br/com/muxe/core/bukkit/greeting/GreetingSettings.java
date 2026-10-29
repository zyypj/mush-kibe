package br.com.muxe.core.bukkit.greeting;

import br.com.muxe.core.common.config.source.DurationParser;
import br.com.muxe.core.common.server.ServerKind;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

public record GreetingSettings(
    Set<ServerKind> kinds,
    long delayTicks,
    Sound sound,
    float volume,
    float pitch,
    int fadeInTicks,
    int stayTicks,
    int fadeOutTicks
) {

    private static final long MILLIS_PER_TICK = 50;

    public GreetingSettings {
        kinds = Set.copyOf(kinds);
    }

    public static GreetingSettings from(ConfigurationSection section) {
        if (section == null) {
            return new GreetingSettings(Set.of(), 0, null, 0f, 0f, 0, 0, 0);
        }
        return new GreetingSettings(
            kinds(section),
            Math.max(0, DurationParser.parse(section.getString("delay", "500ms")).toMillis() / MILLIS_PER_TICK),
            sound(section.getString("sound", "")),
            (float) section.getDouble("volume", 1.0),
            (float) section.getDouble("pitch", 1.0),
            section.getInt("fade-in", 10),
            section.getInt("stay", 50),
            section.getInt("fade-out", 20)
        );
    }

    public boolean appliesTo(ServerKind kind) {
        return kinds.contains(kind);
    }

    private static Set<ServerKind> kinds(ConfigurationSection section) {
        Set<ServerKind> kinds = EnumSet.noneOf(ServerKind.class);
        for (String name : section.getStringList("server-kinds")) {
            try {
                kinds.add(ServerKind.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown server kind '" + name + "' at join-greeting.server-kinds", exception);
            }
        }
        return kinds;
    }

    private static Sound sound(String name) {
        if (name.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown sound '" + name + "' at join-greeting.sound", exception);
        }
    }
}
