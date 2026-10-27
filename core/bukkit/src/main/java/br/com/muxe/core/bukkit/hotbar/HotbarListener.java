package br.com.muxe.core.bukkit.hotbar;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.visibility.VisibilityService;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HotbarListener implements Listener {

    private final Hotbar hotbar;
    private final HotbarActions actions;
    private final VisibilityService visibility;
    private final SpamGuard spam;
    private final BuildMode build;
    private final MessageCatalog messages;

    public HotbarListener(Hotbar hotbar, HotbarActions actions, VisibilityService visibility, SpamGuard spam, BuildMode build, MessageCatalog messages) {
        this.hotbar = hotbar;
        this.actions = actions;
        this.visibility = visibility;
        this.spam = spam;
        this.build = build;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        hotbar.give(player, Visibility.ALL);
        player.getInventory().setHeldItemSlot(0);
        visibility.joined(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (build.isBuilding(player)) {
            return;
        }
        Optional<HotbarAction> action = hotbar.actionFor(player.getInventory().getHeldItemSlot(), event.getItem());
        if (action.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        SpamDecision decision = spam.use(player.getUniqueId());
        if (!decision.allowed()) {
            player.sendMessage(messages.get("spam-cooldown", "seconds", CooldownFormat.seconds(decision.remaining())));
            return;
        }
        actions.run(action.get(), player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spam.forget(event.getPlayer().getUniqueId());
        visibility.left(event.getPlayer());
    }
}
