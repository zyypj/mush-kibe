package br.com.muxe.minigame.api.listener;

import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.runtime.Playing;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public interface GameHandlers<S extends MatchState> {

    default Optional<UUID> attacker(Entity damager) {
        return Optional.empty();
    }

    default boolean selfDamage(Playing<S> victim, Entity damager, EntityDamageEvent event) {
        return false;
    }

    default void damaged(Playing<S> victim, Optional<Participant> attacker, EntityDamageByEntityEvent event) {
    }

    default void interact(Playing<S> playing, PlayerInteractEvent event) {
    }

    default void place(Playing<S> playing, BlockPlaceEvent event) {
    }

    default void breakBlock(Playing<S> playing, BlockBreakEvent event) {
    }

    default boolean explodes(Block block) {
        return true;
    }

    default void exploded(Optional<MatchRuntime<S>> runtime, EntityExplodeEvent event) {
    }

    default void pickup(Playing<S> playing, PlayerPickupItemEvent event) {
    }

    default void consume(Playing<S> playing, PlayerItemConsumeEvent event) {
    }

    default void openMenu(Player player) {
    }

    default void playAgain(Player player, String mode) {
    }
}
