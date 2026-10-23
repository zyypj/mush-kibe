package br.com.muxe.core.bukkit.config;

import br.com.muxe.core.common.config.source.ConfigSource;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public final class BukkitConfigSource implements ConfigSource {

    private final ConfigurationSection section;

    public BukkitConfigSource(ConfigurationSection section) {
        this.section = section;
    }

    @Override
    public Optional<Object> find(String path) {
        if (!section.isSet(path)) {
            return Optional.empty();
        }
        return Optional.ofNullable(section.get(path));
    }
}
