package br.com.muxe.core.bukkit.npc.placed;

import br.com.muxe.core.bukkit.network.GameConnector;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcLabelLayout;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.placement.Reconciler;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.placement.domain.HeldItem;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class PlacedNpcs {

    private final Plugin plugin;
    private final GameConnector games;
    private final NpcLines lines;
    private final SpamGuard spam;
    private final MessageCatalog messages;
    private final Consumer<Player> openShop;
    private final NpcService service;
    private final Reconciler<NpcPlacement> shown = new Reconciler<>(NpcPlacement::id);
    private final Map<String, NpcPlacement> byId = new ConcurrentHashMap<>();

    public PlacedNpcs(Plugin plugin, GameConnector games, NpcLines lines, NpcLabelLayout layout, SpamGuard spam, MessageCatalog messages, Consumer<Player> openShop) {
        this.plugin = plugin;
        this.games = games;
        this.lines = lines;
        this.spam = spam;
        this.messages = messages;
        this.openShop = openShop;
        this.service = NpcService.start(plugin, layout, this::linesOf);
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
                service.put(new Npc(npc.id(), location.get(), npc.skin().map(PlacedNpcs::skin), npc.hand().flatMap(this::item)));
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

    public static Optional<ItemStack> hand(HeldItem item) {
        return Optional.ofNullable(Material.matchMaterial(item.material()))
            .filter(material -> material != Material.AIR)
            .map(material -> new ItemStack(material, 1, (short) item.data()));
    }

    private Optional<ItemStack> item(HeldItem held) {
        Optional<ItemStack> item = hand(held);
        if (item.isEmpty()) {
            plugin.getLogger().warning("NPC item '" + held.material() + "' is not a 1.8 material; the NPC holds nothing");
        }
        return item;
    }

    private List<String> linesOf(Npc npc) {
        NpcPlacement placement = byId.get(npc.id());
        if (placement == null) {
            return List.of();
        }
        return lines.render(placement, games.players(placement.action().target()));
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
                    games.connect(player, placement.action().target(), placement.name());
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
}
