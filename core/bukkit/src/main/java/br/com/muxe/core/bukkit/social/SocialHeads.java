package br.com.muxe.core.bukkit.social;

import br.com.muxe.core.bukkit.display.Display;
import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.nms.BlockUpdates;
import br.com.muxe.core.bukkit.nms.SkullTextures;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.placement.Reconciler;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public final class SocialHeads implements Listener, Runnable {

    public static final String DISPLAY_PREFIX = "social-head:";
    public static final long CHECK_PERIOD_TICKS = 20L;

    private static final byte FLOOR = 1;
    private static final double CENTER = 0.5;

    private final Plugin plugin;
    private final SocialHeadSettings settings;
    private final SpamGuard spam;
    private final MessageCatalog messages;
    private final Clock clock;
    private final DisplayService displays;
    private final Reconciler<SocialHeadPlacement> shown = new Reconciler<>(SocialHeadPlacement::id);
    private final Map<String, Block> blocks = new ConcurrentHashMap<>();
    private int current;

    public SocialHeads(Plugin plugin, SocialHeadSettings settings, SpamGuard spam, MessageCatalog messages, Clock clock) {
        this.plugin = plugin;
        this.settings = settings;
        this.spam = spam;
        this.messages = messages;
        this.clock = clock;
        this.current = settings.indexAt(clock.instant());
        this.displays = DisplayService.start(plugin);
        this.displays.onClick((player, display) -> clicked(player));
    }

    public void show(List<SocialHeadPlacement> heads) {
        Reconciler.Changes<SocialHeadPlacement> changes = shown.next(heads);
        changes.removed().forEach(this::clear);
        for (SocialHeadPlacement head : changes.put()) {
            Optional<Location> location = Positions.resolve(head.position(), plugin.getServer()::getWorld);
            if (location.isEmpty()) {
                clear(head.id());
                plugin.getLogger().warning("Social head '" + head.id() + "' is in world '" + head.position().world() + "', which is not loaded");
                continue;
            }
            Block block = location.get().getBlock();
            Block previous = blocks.put(head.id(), block);
            if (previous != null && !previous.equals(block)) {
                removeSkull(previous);
            }
            place(head, block);
        }
    }

    @Override
    public void run() {
        int now = settings.indexAt(clock.instant());
        if (now == current) {
            return;
        }
        current = now;
        for (SocialHeadPlacement head : shown.all()) {
            Block block = blocks.get(head.id());
            if (block != null) {
                place(head, block);
            }
        }
    }

    public SocialNetwork network() {
        return settings.networks().get(current);
    }

    public void stop() {
        displays.stop();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (isHead(event.getClickedBlock())) {
            event.setCancelled(true);
            clicked(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isHead(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean isHead(Block block) {
        return block != null && blocks.containsValue(block);
    }

    private void clicked(Player player) {
        SpamDecision decision = spam.use(player.getUniqueId());
        if (!decision.allowed()) {
            player.sendMessage(messages.get("spam-cooldown", "seconds", CooldownFormat.seconds(decision.remaining())));
            return;
        }
        network().message().forEach(line -> player.sendMessage(Colors.translate(line)));
    }

    private void place(SocialHeadPlacement head, Block block) {
        SocialNetwork network = network();
        if (block.getType() != Material.SKULL) {
            block.setType(Material.SKULL, false);
        }
        block.setData(FLOOR, false);
        BlockState state = block.getState();
        if (state instanceof Skull skull) {
            skull.setSkullType(SkullType.PLAYER);
            skull.setRotation(SkullRotation.facing(head.position().yaw()));
            SkullTextures.apply(skull, network.texture());
            skull.update(true, false);
            BlockUpdates.resend(block);
        }
        Location anchor = block.getLocation().add(CENTER, 0, CENTER);
        displays.put(new Display(DISPLAY_PREFIX + head.id(), anchor, List.of(), List.of(new DisplayLabel(network.label(), settings.labelHeight()))));
    }

    private void clear(String id) {
        displays.remove(DISPLAY_PREFIX + id);
        Block block = blocks.remove(id);
        if (block != null) {
            removeSkull(block);
        }
    }

    private static void removeSkull(Block block) {
        if (block.getType() == Material.SKULL) {
            block.setType(Material.AIR, false);
        }
    }
}
