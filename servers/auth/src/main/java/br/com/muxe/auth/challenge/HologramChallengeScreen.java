package br.com.muxe.auth.challenge;

import br.com.muxe.core.bukkit.hologram.PixelHolograms;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public final class HologramChallengeScreen implements ChallengeScreen {

    private final PixelHolograms holograms;

    public HologramChallengeScreen(PixelHolograms holograms) {
        this.holograms = holograms;
    }

    @Override
    public Optional<SpawnedHologram> show(UUID playerId, Location anchor, String code) {
        return Optional.ofNullable(Bukkit.getPlayer(playerId))
            .filter(player -> player.isOnline() && player.getWorld().equals(anchor.getWorld()))
            .map(player -> holograms.show(player, anchor, code));
    }
}
