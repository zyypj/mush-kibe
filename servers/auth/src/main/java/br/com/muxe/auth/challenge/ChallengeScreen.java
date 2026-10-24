package br.com.muxe.auth.challenge;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

public interface ChallengeScreen {

    Optional<SpawnedHologram> show(UUID playerId, Location anchor, String code);
}
