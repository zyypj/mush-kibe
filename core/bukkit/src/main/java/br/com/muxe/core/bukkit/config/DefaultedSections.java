package br.com.muxe.core.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

public final class DefaultedSections {

    private DefaultedSections() {
    }

    public static ConfigurationSection of(ConfigurationSection config, String path) {
        Object own = config.get(path, null);
        if (own instanceof ConfigurationSection section && !section.getKeys(false).isEmpty()) {
            return section;
        }
        ConfigurationSection defaults = config.getDefaultSection();
        ConfigurationSection fallback = defaults == null ? null : defaults.getConfigurationSection(path);
        if (fallback != null) {
            return fallback;
        }
        return own instanceof ConfigurationSection section ? section : new MemoryConfiguration();
    }
}
