package br.com.muxe.core.bukkit.social;

import br.com.muxe.core.common.config.source.DurationParser;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

public record SocialHeadSettings(Duration interval, double labelHeight, List<SocialNetwork> networks) {

    public SocialHeadSettings {
        Objects.requireNonNull(interval, "interval");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("The social head interval must be positive");
        }
        networks = List.copyOf(networks);
        if (networks.isEmpty()) {
            throw new IllegalArgumentException("The social head needs at least one network");
        }
    }

    public static SocialHeadSettings from(ConfigurationSection section) {
        List<SocialNetwork> networks = new ArrayList<>();
        ConfigurationSection list = Objects.requireNonNull(section.getConfigurationSection("networks"), "Missing configuration section " + section.getCurrentPath() + ".networks");
        for (String key : list.getKeys(false)) {
            networks.add(SocialNetwork.from(Objects.requireNonNull(list.getConfigurationSection(key), list.getCurrentPath() + "." + key)));
        }
        return new SocialHeadSettings(
            DurationParser.parse(section.getString("interval", "1m")),
            section.getDouble("label-height", 0.5125),
            networks
        );
    }

    public int indexAt(Instant now) {
        return (int) Math.floorMod(now.toEpochMilli() / interval.toMillis(), (long) networks.size());
    }

    public SocialNetwork at(Instant now) {
        return networks.get(indexAt(now));
    }
}
