package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.config.ConfigPathSuggestions;
import br.com.muxe.core.bukkit.command.config.ConfigReplies;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.config.PluginConfig;
import br.com.muxe.core.bukkit.config.edit.ConfigEditor;
import br.com.muxe.core.bukkit.config.edit.ConfigReloads;
import br.com.muxe.core.bukkit.config.edit.PluginConfigStore;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.mission.config.MissionConfig;
import br.com.muxe.core.bukkit.mission.menu.MissionMenus;
import br.com.muxe.core.bukkit.npc.NpcLabelLayout;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.prompt.AnvilPrompts;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.mission.MissionModule;
import br.com.muxe.core.common.mission.domain.Mission;
import br.com.muxe.minigame.api.arena.ArenaSchema;
import br.com.muxe.minigame.api.bootstrap.MatchServer;
import br.com.muxe.minigame.api.bootstrap.MinigameBootstrap;
import br.com.muxe.minigame.api.cosmetic.aura.AuraTask;
import br.com.muxe.minigame.api.cosmetic.trail.TrailTask;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.api.view.EndTitles;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.bedwars.arena.BedWarsSchema;
import br.com.muxe.minigame.bedwars.command.BedWarsCommand;
import br.com.muxe.minigame.bedwars.command.BedWarsConfigCommand;
import br.com.muxe.minigame.bedwars.custom.BedWarsRoomSchema;
import br.com.muxe.minigame.bedwars.game.Activity;
import br.com.muxe.minigame.bedwars.game.Slingshot;
import br.com.muxe.minigame.bedwars.hotbar.HotbarChoices;
import br.com.muxe.minigame.bedwars.hotbar.HotbarDelivery;
import br.com.muxe.minigame.bedwars.hotbar.HotbarModule;
import br.com.muxe.minigame.bedwars.hotbar.Hotbars;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarEditor;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarIcons;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarMenuSettings;
import br.com.muxe.minigame.bedwars.invisibility.InvisiblePlayers;
import br.com.muxe.minigame.bedwars.listener.ActivityListener;
import br.com.muxe.minigame.bedwars.listener.BedWarsHandlers;
import br.com.muxe.minigame.bedwars.listener.Bedbugs;
import br.com.muxe.minigame.bedwars.listener.ChestListener;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.menu.LobbyMenus;
import br.com.muxe.minigame.bedwars.mission.BedWarsGoal;
import br.com.muxe.minigame.bedwars.mission.MissionRules;
import br.com.muxe.minigame.bedwars.preferences.PlayerPreferences;
import br.com.muxe.minigame.bedwars.preferences.PreferencesModule;
import br.com.muxe.minigame.bedwars.preferences.menu.PreferencesMenu;
import br.com.muxe.minigame.bedwars.quickbuy.QuickBuyModule;
import br.com.muxe.minigame.bedwars.quickbuy.domain.QuickBuy;
import br.com.muxe.minigame.bedwars.room.BedWarsGame;
import br.com.muxe.minigame.bedwars.room.BedWarsMatch;
import br.com.muxe.minigame.bedwars.room.BedWarsServices;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.shop.QuickBuyEditor;
import br.com.muxe.minigame.bedwars.shop.QuickBuys;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.ShopService;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import br.com.muxe.minigame.bedwars.stats.StatsModule;
import br.com.muxe.minigame.bedwars.stats.domain.BedWarsStatsRepository;
import br.com.muxe.minigame.bedwars.stats.infrastructure.postgres.PostgresBedWarsStatsRepository;
import br.com.muxe.minigame.bedwars.teamcolor.FavouriteColors;
import br.com.muxe.minigame.bedwars.teamcolor.TeamColorModule;
import br.com.muxe.minigame.bedwars.teamcolor.menu.FavouriteColorMenu;
import br.com.muxe.minigame.bedwars.teamcolor.menu.FavouriteColorMenuSettings;
import br.com.muxe.minigame.bedwars.tnt.TntTimerSettings;
import br.com.muxe.minigame.bedwars.tnt.TntTimers;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.level.LevelUps;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.GameModeLoader;
import br.com.muxe.minigame.engine.setup.session.SetupMessages;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BedWarsBootstrap {

    public static final String GAME = BedWarsMatch.GAME;
    public static final ZoneId STATS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final JavaPlugin plugin;
    private MinigameBootstrap minigame;
    private CosmeticFeatures cosmetics;
    private LobbyFeatures lobbyFeatures;
    private NpcService npcs;
    private DisplayService displays;
    private Bedbugs bedbugs;
    private AuraTask auras;
    private TrailTask trails;

    public BedWarsBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MinigameEngine enable() {
        FileConfiguration config = PluginConfig.load(plugin);
        LocalizedConfig texts = LocalizedConfig.load(plugin);
        MuxeCore core = MuxeCore.get();
        MessageCatalog messages = SetupMessages.withOverrides(core.messages().merge(MessageCatalog.from(texts, "messages")));
        ConfigurationSection modeSection = section(config, "modes");
        List<GameMode> modes = GameModeLoader.modes(texts);
        ArenaSchema schema = BedWarsSchema.from(modes, modeSection);
        Localized<BedWarsSettings> settings = BedWarsSettings.from(texts);
        Commands commands = Commands.of(plugin, messages);
        minigame = new MinigameBootstrap(plugin, core, GAME, modes, messages, commands, texts);
        boolean lobby = minigame.isLobby();
        plugin.getLogger().info("Schema '" + GAME + "' at version " + core.migrations().migrate(GAME, plugin.getClass().getClassLoader()).schemaVersion());
        StatsModule.register(core.commands(), core.queries(), core.transactions(), core.clock(), STATS_ZONE);
        QuickBuyModule.register(core.commands(), core.queries(), core.transactions(), core.clock());
        HotbarModule.register(core.commands(), core.queries(), core.transactions(), core.clock());
        PreferencesModule.register(core.commands(), core.queries(), core.transactions(), core.clock());
        TeamColorModule.register(core.commands(), core.queries(), core.transactions(), core.clock());
        Localized<MissionConfig> missions = texts.map(root -> MissionConfig.from(root.getConfigurationSection("missions"), BedWarsGoal::find));
        BedWarsStatsRepository missionXp = new PostgresBedWarsStatsRepository();
        core.missions().register(GAME, missions.standard().catalog(),
            (playerId, completed, at) -> missionXp.addXp(playerId, completed.stream().mapToLong(Mission::xp).sum(), at, STATS_ZONE));
        MissionMenus missionMenus = new MissionMenus(GAME, missions, core.languages(), core.queries(), core.mainThread(), core.clock(), MissionModule.ZONE,
            plugin.getLogger());
        EndTitles titles = minigame.endTitles();
        ConfigReloads reloads = new ConfigReloads();
        ConfigEditor configEditor = new ConfigEditor(new PluginConfigStore(plugin, core.async()), reloads);
        ConfigPathSuggestions.register(commands, configEditor);
        commands.register(new BedWarsConfigCommand(configEditor, new ConfigReplies(messages)));
        cosmetics = CosmeticFeatures.start(plugin, core, texts, messages, commands, lobby, titles, reloads);
        cosmetics.onMissions(missionMenus);
        Localized<ShopCatalog> shops = texts.map(root -> ShopCatalog.from(section(root, "shop")));
        ShopCatalog shopCatalog = shops.standard();
        QuickBuys quickBuys = new QuickBuys(core, plugin.getLogger(), QuickBuy.of(shopCatalog.quickBuy()), shopCatalog.items()::containsKey);
        plugin.getServer().getPluginManager().registerEvents(quickBuys, plugin);
        plugin.getServer().getOnlinePlayers().forEach(player -> quickBuys.load(player.getUniqueId()));
        cosmetics.onQuickBuy(new QuickBuyEditor(shops, texts.map(root -> QuickBuyEditor.Settings.from(section(root, "shop.quick-buy-editor"))), quickBuys,
            messages, AnvilPrompts.start(plugin), core.mainThread(), plugin.getLogger()));
        Localized<HotbarChoices> choices = shops.map(HotbarChoices::new);
        HotbarChoices hotbarChoices = choices.standard();
        Hotbars hotbars = new Hotbars(core, plugin.getLogger(), hotbarChoices::exists);
        plugin.getServer().getPluginManager().registerEvents(hotbars, plugin);
        plugin.getServer().getOnlinePlayers().forEach(player -> hotbars.load(player.getUniqueId()));
        Localized<HotbarMenuSettings> hotbarMenus = texts.map(root -> HotbarMenuSettings.from(section(root, "shop.hotbar-menus")));
        Localized<HotbarIcons> hotbarIcons = Localized.of(language -> new HotbarIcons(shops.in(language), choices.in(language), hotbarMenus.in(language).adding()));
        HotbarEditor hotbarEditor = new HotbarEditor(hotbarMenus, choices, hotbarIcons, core.languages(), hotbars, messages);
        cosmetics.onHotbar(hotbarEditor);
        PlayerPreferences preferences = new PlayerPreferences(core, plugin.getLogger());
        plugin.getServer().getPluginManager().registerEvents(preferences, plugin);
        plugin.getServer().getOnlinePlayers().forEach(player -> preferences.load(player.getUniqueId()));
        cosmetics.onPreferences(new PreferencesMenu(texts.map(root -> PreferencesMenu.Settings.from(section(root, "preferences"))), core.languages(), preferences));
        FavouriteColors teamColors = new FavouriteColors(core, plugin.getLogger());
        plugin.getServer().getPluginManager().registerEvents(teamColors, plugin);
        plugin.getServer().getOnlinePlayers().forEach(player -> teamColors.load(player.getUniqueId()));
        cosmetics.onTeamColor(new FavouriteColorMenu(texts.map(root -> FavouriteColorMenuSettings.from(section(root, "favourite-color"))), teamColors, messages));
        if (lobby) {
            lobbyFeatures = LobbyFeatures.start(plugin, core, texts, messages, schema, minigame.modeNames(), missionMenus, preferences, minigame.connector());
            minigame.setup(schema);
            plugin.getLogger().info("BedWars ready in lobby mode");
            return minigame.engine();
        }

        Map<String, Localized<List<String>>> npcLines = new ConcurrentHashMap<>();
        npcs = NpcService.startForViewers(plugin, NpcLabelLayout.STANDARD,
            (viewer, npc) -> Optional.ofNullable(npcLines.get(npc.id())).map(lines -> lines.in(core.languages().of(viewer))).orElse(List.of()));
        displays = DisplayService.start(plugin);
        LevelBadges badges = LevelBadges.from(config.getConfigurationSection("levels"));
        BedWarsServices services = new BedWarsServices(plugin, core, settings, core.languages(), messages, npcs, npcLines, displays, missions,
            MissionRules.from(config.getConfigurationSection("missions")), preferences, cosmetics.messageSets(), cosmetics.killMessages(), cosmetics.bedBreaks(),
            new LevelUps(messages.text("level-up"), badges::of, core.languages()), badges);
        MatchServer<BedWarsArena, BedWarsMatch> server = minigame.matches(new BedWarsGame(services, teamColors::preferred), cosmetics);
        MatchService<BedWarsArena, BedWarsMatch> rooms = server.service();
        Localized<UpgradeCatalog> upgrades = texts.map(root -> UpgradeCatalog.from(section(root, "upgrades")));
        MatchSettings matchSettings = MatchSettings.from(config);
        KnockbackProfile knockback = matchSettings.knockback().profile(matchSettings.defaultMode(), core.knockback().profiles()).orElse(KnockbackProfile.VANILLA);
        String roomSchema = BedWarsRoomSchema.build(Localized.of(language -> new BedWarsRoomSchema.Texts(DefaultedSections.of(texts.in(language), "custom-room"),
            shops.in(language), upgrades.in(language))), settings.standard().timeline(), knockback);
        server.customRooms(() -> roomSchema);
        ShopService shop = new ShopService(
            shops,
            upgrades,
            messages,
            settings.standard().sounds(),
            quickBuys,
            settings.map(BedWarsSettings::items),
            new HotbarDelivery(hotbars, hotbarChoices),
            hotbarEditor,
            cosmetics.woods()
        );
        npcs.onClick((player, npc) -> rooms.of(player.getUniqueId()).ifPresent(runtime -> {
            if (!runtime.state().hasNpc(npc.id()) || runtime.match().phase() != MatchPhase.PLAYING || runtime.isSpectator(player.getUniqueId())) {
                return;
            }
            shop.open(player, runtime, npc.id().contains("-melhorias"));
        }));
        LobbyMenus playAgain = PlayAgain.menus(plugin, core, texts, messages, schema, minigame.modeNames(), server.playAgainConnector());
        bedbugs = new Bedbugs(plugin, rooms, core.clock());
        bedbugs.start();
        TntTimers tntTimers = TntTimers.start(plugin, texts.map(root -> TntTimerSettings.from(section(root, "tnt-timer"))), core.languages(), preferences);
        Activity activity = new Activity();
        Slingshot slingshot = new Slingshot();
        BedWarsHandlers handlers = new BedWarsHandlers(plugin, core, settings, messages, bedbugs, preferences, tntTimers, activity, slingshot,
            cosmetics::openMenu, playAgain::openMode);
        server.listen(handlers);
        core.languages().onChange(player -> rooms.of(player.getUniqueId()).ifPresent(runtime -> runtime.state().languageChanged(player.getUniqueId())));
        plugin.getServer().getPluginManager().registerEvents(new ChestListener(rooms, handlers), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ActivityListener(activity, slingshot, core.clock()), plugin);
        InvisiblePlayers.start(plugin, rooms);
        auras = AuraTask.start(plugin, rooms, cosmetics.auras());
        trails = TrailTask.start(plugin, rooms, cosmetics.trails());
        commands.register(new BedWarsCommand(rooms, messages));

        plugin.getLogger().info("BedWars ready with modes " + minigame.engine().modes().keySet());
        return minigame.engine();
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + path);
    }

    public void disable() {
        if (lobbyFeatures != null) {
            lobbyFeatures.stop();
        }
        if (auras != null) {
            auras.stop();
        }
        if (trails != null) {
            trails.stop();
        }
        if (bedbugs != null) {
            bedbugs.stop();
        }
        if (cosmetics != null) {
            cosmetics.stop();
        }
        if (minigame != null) {
            minigame.stopSetup();
            minigame.stopMatches();
        }
        if (npcs != null) {
            npcs.stop();
        }
        if (displays != null) {
            displays.stop();
        }
    }
}
