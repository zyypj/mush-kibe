package br.com.muxe.lobby.npc;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import br.com.muxe.core.bukkit.npc.skin.MojangSkins;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class LobbyNpcs {

    private final Plugin plugin;
    private final MuxeCore core;
    private final NpcStore store;
    private final MojangSkins skins;
    private final SpamGuard spam;
    private final MessageCatalog messages;
    private final List<String> lineTemplate;
    private final Map<String, GameNpc> npcs = new LinkedHashMap<>();
    private final NpcService service;

    public LobbyNpcs(Plugin plugin, MuxeCore core, NpcStore store, MojangSkins skins, SpamGuard spam, MessageCatalog messages, List<String> lineTemplate) {
        this.plugin = plugin;
        this.core = core;
        this.store = store;
        this.skins = skins;
        this.spam = spam;
        this.messages = messages;
        this.lineTemplate = List.copyOf(lineTemplate);
        this.service = NpcService.start(plugin, this::linesOf);
        this.service.onClick(this::clicked);
    }

    public void load() {
        npcs.clear();
        npcs.putAll(store.load());
        npcs.values().forEach(this::place);
    }

    public void stop() {
        service.stop();
    }

    public Optional<GameNpc> find(String id) {
        return Optional.ofNullable(npcs.get(id));
    }

    public Collection<GameNpc> all() {
        return List.copyOf(npcs.values());
    }

    public CompletableFuture<Optional<NpcSkin>> skin(String name) {
        return skins.byName(name);
    }

    public void save(GameNpc npc) {
        npcs.put(npc.id(), npc);
        store.save(npcs.values());
        place(npc);
    }

    public void remove(String id) {
        if (npcs.remove(id) != null) {
            store.save(npcs.values());
            service.remove(id);
        }
    }

    public static StoredLocation standingAt(Location location) {
        return StoredLocation.of(location);
    }

    public int playersIn(String group) {
        return core.servers().aliveInGroup(group).stream().mapToInt(ServerStatus::onlinePlayers).sum();
    }

    private void place(GameNpc npc) {
        Optional<Npc> placed = npc.toNpc(plugin.getServer()::getWorld);
        if (placed.isPresent()) {
            service.put(placed.get());
        } else {
            service.remove(npc.id());
            plugin.getLogger().warning("NPC '" + npc.id() + "' is in world '" + npc.location().world() + "', which is not loaded");
        }
    }

    private List<String> linesOf(Npc npc) {
        return find(npc.id()).map(game -> game.lines(lineTemplate, playersIn(game.group()))).orElse(List.of());
    }

    private void clicked(Player player, Npc npc) {
        Optional<GameNpc> game = find(npc.id());
        if (game.isEmpty()) {
            return;
        }
        SpamDecision decision = spam.use(player.getUniqueId());
        if (!decision.allowed()) {
            player.sendMessage(messages.get("spam-cooldown", "seconds", CooldownFormat.seconds(decision.remaining())));
            return;
        }
        String group = game.get().group();
        String name = game.get().name();
        if (core.servers().leastLoaded(group).isEmpty()) {
            player.sendMessage(messages.get("npc-unavailable", "name", name));
            return;
        }
        player.sendMessage(messages.get("npc-connecting", "name", name));
        core.network().connectToGroup(player.getUniqueId(), group).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get("play-failed")));
            return null;
        });
    }
}
