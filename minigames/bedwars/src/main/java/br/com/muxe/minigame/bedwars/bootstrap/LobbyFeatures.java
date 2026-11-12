package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.mission.menu.MissionMenus;
import br.com.muxe.core.bukkit.npc.NpcLabelLayout;
import br.com.muxe.core.bukkit.scheduledgame.ScheduledGame;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.arena.ArenaSchema;
import br.com.muxe.minigame.api.matchmaking.MatchConnector;
import br.com.muxe.minigame.bedwars.lobby.LevelChat;
import br.com.muxe.minigame.bedwars.lobby.LobbyNpcs;
import br.com.muxe.minigame.bedwars.lobby.LobbySidebar;
import br.com.muxe.minigame.bedwars.lobby.StatsCache;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.menu.LobbyMenuSettings;
import br.com.muxe.minigame.bedwars.lobby.menu.LobbyMenus;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyNpc;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyScoreboardSettings;
import br.com.muxe.minigame.bedwars.lobby.settings.StatsNpcSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TagColors;
import br.com.muxe.minigame.bedwars.lobby.top.TopBoardSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopBoards;
import br.com.muxe.minigame.bedwars.lobby.top.TopData;
import br.com.muxe.minigame.bedwars.lobby.top.TopSelectorSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopTexts;
import br.com.muxe.minigame.bedwars.preferences.PlayerPreferences;
import br.com.muxe.minigame.engine.level.LevelUps;
import br.com.muxe.minigame.engine.mode.ModeNames;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyFeatures {

    public static final String DUELS_ACTION = "duelos";
    public static final String NO_ACTION = "nenhuma";
    public static final String MISSIONS_ACTION = "missoes";

    private final MuxeCore core;
    private final LobbyNpcs npcs;
    private final TopBoards boards;

    private LobbyFeatures(MuxeCore core, LobbyNpcs npcs, TopBoards boards) {
        this.core = core;
        this.npcs = npcs;
        this.boards = boards;
    }

    public static LobbyFeatures start(JavaPlugin plugin, MuxeCore core, LocalizedConfig texts, MessageCatalog messages, ArenaSchema schema, ModeNames modeNames,
                                      MissionMenus missions, PlayerPreferences preferences, MatchConnector connector) {
        ConfigurationSection lobby = section(texts.standard(), "lobby");
        LevelBadges badges = LevelBadges.from(section(texts.standard(), "levels"));
        Localized<TopTexts> topTexts = texts.map(root -> TopTexts.from(section(root, "lobby.tops")));
        StatsCache stats = new StatsCache(plugin, core, new LevelUps(messages.text("level-up"), badges::of, core.languages()));
        stats.start();
        plugin.getServer().getPluginManager().registerEvents(new LevelChat(stats, badges, preferences), plugin);
        TagColors colors = new TagColors(core);
        TopData tops = new TopData(plugin, core, Duration.ofSeconds(topTexts.standard().refreshSeconds()));
        LobbyMenus menus = new LobbyMenus(plugin, core, texts.map(root -> LobbyMenuSettings.from(section(root, "lobby.menus"))), schema, modeNames, connector, tops,
            colors, badges, messages);
        LobbyNpcs npcs = new LobbyNpcs(plugin, texts.map(root -> LobbyNpc.from(section(root, "lobby.npcs"))), NpcLabelLayout.from(lobby.getConfigurationSection("npc-labels")),
            lobby.getStringList("blink-colors"), texts.map(root -> StatsNpcSettings.from(section(root, "lobby.stats-npc"))), core.languages(), connector.directory(), stats,
            badges, (player, action) -> act(core, player, action, menus, schema, missions));
        npcs.start();
        TopBoards boards = new TopBoards(plugin, texts.map(root -> TopBoardSettings.from(section(root, "lobby.tops.boards"))), topTexts, tops, colors, badges,
            menus::modeTitle, menus::openTopModes, texts.map(root -> TopSelectorSettings.from(root.getConfigurationSection("lobby.tops.selector"))),
            menus.topModeOrder(), menus::modeLabel, core.languages());
        boards.start();
        core.languages().onChange(boards::refresh);
        Localized<LobbyScoreboardSettings> scoreboards = texts.map(root -> LobbyScoreboardSettings.from(section(root, "lobby.scoreboard")));
        core.sidebars().provide(player -> {
            Language language = core.languages().of(player);
            return Optional.of(LobbySidebar.content(scoreboards.in(language), stats.of(player.getUniqueId()), badges, schema.modes(),
                mode -> menus.modeName(language, mode), core.servers().gamePlayers(BedWarsBootstrap.GAME)));
        });
        return new LobbyFeatures(core, npcs, boards);
    }

    public void stop() {
        core.sidebars().clear();
        npcs.stop();
        boards.stop();
    }

    private static void act(MuxeCore core, Player player, String action, LobbyMenus menus, ArenaSchema schema, MissionMenus missions) {
        Optional<ScheduledGame> scheduled = core.scheduledGames().of(BedWarsBootstrap.GAME, action);
        if (scheduled.isPresent()) {
            core.scheduledPlay().play(player, scheduled.get().id());
        } else if (MISSIONS_ACTION.equals(action)) {
            missions.open(player, Optional.empty());
        } else if (DUELS_ACTION.equals(action)) {
            menus.openDuels(player);
        } else if (LobbyNpcs.STATS_ACTION.equals(action)) {
            menus.openStats(player);
        } else if (!NO_ACTION.equals(action) && schema.modes().contains(action)) {
            menus.openMode(player, action);
        }
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
