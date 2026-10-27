package br.com.muxe.core.bukkit.visibility;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.hotbar.Hotbar;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.visibility.application.command.ChangeVisibilityCommand;
import br.com.muxe.core.common.visibility.application.query.FindVisibilityQuery;
import br.com.muxe.core.common.visibility.domain.FriendDirectory;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class VisibilityService {

    private final MuxeCore core;
    private final FriendDirectory friends;
    private final Hotbar hotbar;
    private final MessageCatalog messages;
    private final Logger logger;
    private final Map<UUID, Visibility> settings = new ConcurrentHashMap<>();

    public VisibilityService(MuxeCore core, FriendDirectory friends, Hotbar hotbar, MessageCatalog messages, Logger logger) {
        this.core = core;
        this.friends = friends;
        this.hotbar = hotbar;
        this.messages = messages;
        this.logger = logger;
    }

    public Visibility of(UUID playerId) {
        return settings.getOrDefault(playerId, Visibility.ALL);
    }

    public void joined(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                applyBetween(viewer, player);
            }
        }
        core.queries().ask(new FindVisibilityQuery(player.getUniqueId())).whenComplete((visibility, error) -> core.mainThread().execute(() -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the visibility of " + player.getName() + ": " + error.getMessage());
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            settings.put(player.getUniqueId(), visibility);
            hotbar.showVisibility(player, visibility);
            apply(player);
        }));
    }

    public void left(Player player) {
        settings.remove(player.getUniqueId());
    }

    public void toggle(Player player) {
        Visibility next = of(player.getUniqueId()).next();
        settings.put(player.getUniqueId(), next);
        hotbar.showVisibility(player, next);
        apply(player);
        player.sendMessage(messages.get(switch (next) {
            case ALL -> "visibility-all";
            case FRIENDS -> "visibility-friends";
            case NONE -> "visibility-none";
        }));
        core.commands().dispatch(new ChangeVisibilityCommand(player.getUniqueId(), next)).whenComplete((result, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not save the visibility of " + player.getName() + ": " + error.getMessage());
            }
        });
    }

    private void apply(Player viewer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(viewer)) {
                applyBetween(viewer, other);
            }
        }
    }

    private void applyBetween(Player viewer, Player other) {
        if (of(viewer.getUniqueId()).shows(friends.areFriends(viewer.getUniqueId(), other.getUniqueId()))) {
            viewer.showPlayer(other);
        } else {
            viewer.hidePlayer(other);
        }
    }
}
