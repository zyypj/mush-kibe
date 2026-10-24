package br.com.muxe.lobby.listener;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.lobby.hotbar.HotbarAction;
import br.com.muxe.lobby.hotbar.LobbyHotbar;
import br.com.muxe.lobby.selector.LobbySelectorMenu;
import br.com.muxe.lobby.visibility.VisibilityService;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HotbarListener implements Listener {

    private final LobbyHotbar hotbar;
    private final SpamGuard spam;
    private final VisibilityService visibility;
    private final LobbySelectorMenu selector;
    private final BuildMode build;
    private final MessageCatalog messages;

    public HotbarListener(LobbyHotbar hotbar, SpamGuard spam, VisibilityService visibility, LobbySelectorMenu selector, BuildMode build, MessageCatalog messages) {
        this.hotbar = hotbar;
        this.spam = spam;
        this.visibility = visibility;
        this.selector = selector;
        this.build = build;
        this.messages = messages;
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
        switch (action.get()) {
            case VISIBILITY -> visibility.toggle(player);
            case LOBBY_SELECTOR -> selector.open(player);
            case GAME_SELECTOR, PROFILE, COSMETICS -> {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spam.forget(event.getPlayer().getUniqueId());
    }
}
