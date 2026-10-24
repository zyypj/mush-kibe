package br.com.muxe.core.bukkit.nms;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface EntityUseListener {

    void onUse(Player player, int entityId, EntityUse use);
}
