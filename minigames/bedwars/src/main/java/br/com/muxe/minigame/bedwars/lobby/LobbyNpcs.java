package br.com.muxe.minigame.bedwars.lobby;

import br.com.muxe.core.bukkit.nms.PlayerSkins;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcLabelLayout;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.matchmaking.RoomDirectory;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyNpc;
import br.com.muxe.minigame.bedwars.lobby.settings.StatsNpcSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class LobbyNpcs implements Listener {

    public static final String STATS_PREFIX = "estatisticas:";
    public static final String STATS_ACTION = "estatisticas";
    public static final long BLINK_MILLIS = 500;

    private final Plugin plugin;
    private final Map<String, LobbyNpc> npcs;
    private final Localized<Map<String, LobbyNpc>> translated;
    private final List<String> blinkColors;
    private final StatsNpcSettings statsNpc;
    private final Localized<StatsNpcSettings> statsTexts;
    private final LanguageLookup languages;
    private final NpcLabelLayout labels;
    private final RoomDirectory directory;
    private final StatsCache stats;
    private final LevelBadges badges;
    private final BiConsumer<Player, String> actions;
    private NpcService service;

    public LobbyNpcs(Plugin plugin, Localized<List<LobbyNpc>> npcs, NpcLabelLayout labels, List<String> blinkColors, Localized<StatsNpcSettings> statsNpc,
                     LanguageLookup languages, RoomDirectory directory, StatsCache stats, LevelBadges badges, BiConsumer<Player, String> actions) {
        this.labels = labels;
        this.plugin = plugin;
        this.translated = npcs.map(LobbyNpcs::byId);
        this.npcs = translated.standard();
        this.blinkColors = List.copyOf(blinkColors);
        this.statsNpc = statsNpc.standard();
        this.statsTexts = statsNpc;
        this.languages = languages;
        this.directory = directory;
        this.stats = stats;
        this.badges = badges;
        this.actions = actions;
    }

    public void start() {
        service = NpcService.startForViewers(plugin, npc -> npc.id().startsWith(STATS_PREFIX) ? statsNpc.labels() : labels,
            (viewer, npc) -> lines(npc, languages.of(viewer.getUniqueId())));
        service.visibleTo((viewer, npc) -> !npc.id().startsWith(STATS_PREFIX) || npc.id().equals(STATS_PREFIX + viewer.getUniqueId()));
        service.onClick(this::clicked);
        for (LobbyNpc npc : npcs.values()) {
            service.put(new Npc(npc.id(), npc.spot().toLocation(plugin.getServer()), npc.skin()));
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getOnlinePlayers().forEach(this::addStatsNpc);
    }

    public void stop() {
        if (service != null) {
            service.stop();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        addStatsNpc(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.remove(STATS_PREFIX + event.getPlayer().getUniqueId());
    }

    private void addStatsNpc(Player player) {
        service.put(new Npc(STATS_PREFIX + player.getUniqueId(), statsNpc.spot().toLocation(plugin.getServer()), PlayerSkins.of(player)));
    }

    List<String> lines(Npc npc, Language language) {
        if (npc.id().startsWith(STATS_PREFIX)) {
            UUID owner = UUID.fromString(npc.id().substring(STATS_PREFIX.length()));
            return LobbyLines.stats(statsTexts.in(language).lines(), stats.of(owner).general(), badges, statsNpc.barSegments());
        }
        LobbyNpc lobbyNpc = translated.in(language).get(npc.id());
        if (lobbyNpc == null) {
            return List.of();
        }
        int players = directory.playersIn(Set.copyOf(lobbyNpc.players()));
        return LobbyLines.npc(lobbyNpc.lines(), players, LobbyLines.blink(blinkColors, System.currentTimeMillis(), BLINK_MILLIS));
    }

    private static Map<String, LobbyNpc> byId(List<LobbyNpc> npcs) {
        return npcs.stream().collect(Collectors.toMap(LobbyNpc::id, npc -> npc, (first, second) -> first, LinkedHashMap::new));
    }

    private void clicked(Player player, Npc npc) {
        if (npc.id().startsWith(STATS_PREFIX)) {
            actions.accept(player, STATS_ACTION);
            return;
        }
        Optional.ofNullable(npcs.get(npc.id())).ifPresent(lobbyNpc -> actions.accept(player, lobbyNpc.action()));
    }
}
