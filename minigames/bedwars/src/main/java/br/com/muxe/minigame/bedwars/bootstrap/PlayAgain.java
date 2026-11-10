package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.api.arena.ArenaSchema;
import br.com.muxe.minigame.api.matchmaking.MatchConnector;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.menu.LobbyMenuSettings;
import br.com.muxe.minigame.bedwars.lobby.menu.LobbyMenus;
import br.com.muxe.minigame.bedwars.lobby.top.TagColors;
import br.com.muxe.minigame.bedwars.lobby.top.TopData;
import br.com.muxe.minigame.bedwars.lobby.top.TopTexts;
import br.com.muxe.minigame.engine.mode.ModeNames;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

final class PlayAgain {

    private PlayAgain() {
    }

    static LobbyMenus menus(JavaPlugin plugin, MuxeCore core, LocalizedConfig texts, MessageCatalog messages, ArenaSchema schema, ModeNames modeNames,
                            MatchConnector connector) {
        ConfigurationSection lobby = section(texts.standard(), "lobby");
        TopTexts tops = TopTexts.from(section(lobby, "tops"));
        TopData data = new TopData(plugin, core, Duration.ofSeconds(tops.refreshSeconds()));
        return new LobbyMenus(plugin, core, texts.map(root -> LobbyMenuSettings.from(section(root, "lobby.menus"))), schema, modeNames, connector, data,
            new TagColors(core), LevelBadges.from(texts.standard().getConfigurationSection("levels")), messages);
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + path);
    }
}
