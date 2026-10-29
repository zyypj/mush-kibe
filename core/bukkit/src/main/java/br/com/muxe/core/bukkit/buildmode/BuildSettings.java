package br.com.muxe.core.bukkit.buildmode;

import br.com.muxe.core.common.config.source.DurationParser;
import br.com.muxe.core.common.server.ServerKind;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;

public record BuildSettings(Set<ServerKind> adventureOnlyKinds, Duration duration) {

    public BuildSettings {
        adventureOnlyKinds = Set.copyOf(adventureOnlyKinds);
    }

    public static BuildSettings from(ConfigurationSection section) {
        if (section == null) {
            return new BuildSettings(Set.of(), Duration.ofHours(6));
        }
        Set<ServerKind> kinds = EnumSet.noneOf(ServerKind.class);
        for (String name : section.getStringList("adventure-only")) {
            try {
                kinds.add(ServerKind.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown server kind '" + name + "' at build-mode.adventure-only", exception);
            }
        }
        return new BuildSettings(kinds, DurationParser.parse(section.getString("duration", "6h")));
    }

    public boolean adventureOnly(ServerKind kind) {
        return adventureOnlyKinds.contains(kind);
    }

    public GameMode restingMode(ServerKind kind, GameMode serverDefault) {
        return adventureOnly(kind) ? GameMode.ADVENTURE : serverDefault;
    }
}
