package br.com.muxe.lobby.npc;

import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class NpcStore {

    private final File file;
    private final Logger logger;

    public NpcStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public Map<String, GameNpc> load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, GameNpc> npcs = new LinkedHashMap<>();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                npcs.put(id, read(id, section));
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Ignoring NPC '" + id + "' in " + file.getName() + ": " + exception.getMessage());
            }
        }
        return npcs;
    }

    public void save(Collection<GameNpc> npcs) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (GameNpc npc : npcs) {
            String path = npc.id();
            yaml.set(path + ".group", npc.group());
            yaml.set(path + ".name", npc.name());
            yaml.set(path + ".skin-name", npc.skinName());
            npc.skin().ifPresent(skin -> {
                yaml.set(path + ".skin.value", skin.value());
                yaml.set(path + ".skin.signature", skin.signature());
            });
            npc.location().toMap().forEach((key, value) -> yaml.set(path + ".location." + key, value));
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not save " + file.getName(), exception);
        }
    }

    private static GameNpc read(String id, ConfigurationSection section) {
        StoredLocation location = StoredLocation.from(new BukkitConfigSource(section), "location")
            .orElseThrow(() -> new IllegalArgumentException("missing location"));
        Optional<NpcSkin> skin = Optional.ofNullable(section.getString("skin.value"))
            .flatMap(value -> Optional.ofNullable(section.getString("skin.signature")).map(signature -> new NpcSkin(value, signature)));
        return new GameNpc(id, section.getString("group", id), section.getString("name", id), location, section.getString("skin-name", ""), skin);
    }
}
