package br.com.muxe.minigame.api.spectator;

import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;

public final class Spectating {

    public static final Duration COMBAT_WINDOW = Duration.ofSeconds(5);

    private Spectating() {
    }

    public static void openTeleporter(MatchRuntime<?> runtime, Player viewer) {
        List<Player> targets = runtime.onlinePlayers().stream()
            .filter(other -> !other.equals(viewer) && !runtime.isSpectator(other.getUniqueId()))
            .toList();
        TeleporterMenu.open(viewer, runtime.settings(viewer.getUniqueId()).teleporter(), targets,
            target -> runtime.match().player(target.getUniqueId()).map(runtime::coloredName).orElse(target.getName()));
    }

    public static void teleport(MatchRuntime<?> runtime, Player viewer, boolean inCombat) {
        List<UUID> alive = runtime.online().stream().filter(id -> !runtime.isSpectator(id)).toList();
        Optional<UUID> target = inCombat
            ? SpectatorTargets.inCombat(alive, runtime.match().credit().fighting(runtime.environment().clock().instant(), COMBAT_WINDOW),
                viewer.getUniqueId(), ThreadLocalRandom.current())
            : SpectatorTargets.random(alive, viewer.getUniqueId(), ThreadLocalRandom.current());
        target.flatMap(runtime::player).filter(Player::isOnline).ifPresentOrElse(
            found -> viewer.teleport(found.getLocation()),
            () -> viewer.sendMessage(runtime.message(viewer.getUniqueId(), inCombat ? "spectator-no-combat" : "spectator-no-target")));
    }

    public static Optional<Participant> alive(MatchRuntime<?> runtime, UUID id) {
        return runtime.match().player(id).filter(Participant::isAlive);
    }
}
