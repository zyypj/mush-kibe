package br.com.muxe.core.bukkit.npc.placed;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.placement.Reconciler;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PlacedNpcs {

    private final Plugin plugin;
    private final MuxeCore core;
    private final NpcLines lines;
    private final SpamGuard spam;
    private final MessageCatalog messages;
    private final Consumer<Player> openShop;
    private final NpcService service;
    private final Reconciler<NpcPlacement> shown = new Reconciler<>(NpcPlacement::id);
    private final Map<String, NpcPlacement> byId = new ConcurrentHashMap<>();

    public PlacedNpcs(Plugin plugin, MuxeCore core, NpcLines lines, SpamGuard spam, MessageCatalog messages, Consumer<Player> openShop) {
        this.plugin = plugin;
        this.core = core;
        this.lines = lines;
        this.spam = spam;
        this.messages = messages;
        this.openShop = openShop;
        this.service = NpcService.start(plugin, this::linesOf);
        this.service.onClick(this::clicked);
    }

    public void show(List<NpcPlacement> npcs) {
        Reconciler.Changes<NpcPlacement> changes = shown.next(npcs);
        changes.removed().forEach(id -> {
            byId.remove(id);
            service.remove(id);
        });
        for (NpcPlacement npc : changes.put()) {
            byId.put(npc.id(), npc);
            Optional<Location> location = Positions.resolve(npc.position(), plugin.getServer()::getWorld);
            if (location.isPresent()) {
                service.put(new Npc(npc.id(), location.get(), npc.skin().map(PlacedNpcs::skin)));
            } else {
                service.remove(npc.id());
                plugin.getLogger().warning("NPC '" + npc.id() + "' is in world '" + npc.position().world() + "', which is not loaded");
            }
        }
    }

    public void stop() {
        service.stop();
    }

    public static NpcSkin skin(SkinTexture texture) {
        return new NpcSkin(texture.value(), texture.signature());
    }

    public static SkinTexture texture(NpcSkin skin) {
        return new SkinTexture(skin.value(), skin.signature());
    }

    private List<String> linesOf(Npc npc) {
        NpcPlacement placement = byId.get(npc.id());
        if (placement == null) {
            return List.of();
        }
        return lines.render(placement, playersIn(placement.action().target()));
    }

    private int playersIn(String group) {
        if (group.isEmpty()) {
            return 0;
        }
        return core.servers().aliveInGroup(group).stream().mapToInt(ServerStatus::onlinePlayers).sum();
    }

    private void clicked(Player player, Npc npc) {
        NpcPlacement placement = byId.get(npc.id());
        if (placement == null) {
            return;
        }
        switch (placement.action().type()) {
            case NONE -> {
            }
            case SHOP -> {
                if (allowed(player)) {
                    openShop.accept(player);
                }
            }
            case GAME -> {
                if (allowed(player)) {
                    connect(player, placement);
                }
            }
        }
    }

    private boolean allowed(Player player) {
        SpamDecision decision = spam.use(player.getUniqueId());
        if (!decision.allowed()) {
            player.sendMessage(messages.get("spam-cooldown", "seconds", CooldownFormat.seconds(decision.remaining())));
        }
        return decision.allowed();
    }

    private void connect(Player player, NpcPlacement placement) {
        String group = placement.action().target();
        String name = placement.name();
        if (core.servers().leastLoaded(group).isEmpty()) {
            player.sendMessage(messages.get("npc-unavailable", "name", name));
            return;
        }
        player.sendMessage(messages.get("npc-connecting", "name", name));
        core.network().connectToGroup(player.getUniqueId(), group).exceptionally(error -> {
            core.mainThread().execute(() -> player.sendMessage(messages.get("connect-failed")));
            return null;
        });
    }
}
