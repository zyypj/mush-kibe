package br.com.muxe.auth.setup;

import br.com.muxe.core.bukkit.effect.Anchors;
import br.com.muxe.core.bukkit.location.LocationSlot;
import java.util.Optional;
import org.bukkit.Location;

public final class AuthLocations {

    public static final double EYE_HEIGHT = 1.62;

    private final LocationSlot spawn;
    private final LocationSlot challengeCode;
    private final double defaultCodeDistance;

    public AuthLocations(LocationSlot spawn, LocationSlot challengeCode, double defaultCodeDistance) {
        this.spawn = spawn;
        this.challengeCode = challengeCode;
        this.defaultCodeDistance = defaultCodeDistance;
    }

    public LocationSlot spawn() {
        return spawn;
    }

    public LocationSlot challengeCode() {
        return challengeCode;
    }

    public Optional<Location> spawnLocation() {
        return spawn.location();
    }

    public Location spawnEyes(Location fallbackEyes) {
        return spawn.location().map(location -> location.clone().add(0, EYE_HEIGHT, 0)).orElse(fallbackEyes);
    }

    public Location challengeAnchor(Location viewerEyes, double codeHeight) {
        return challengeCode.location()
            .map(code -> Anchors.lookingAt(code, viewerEyes, codeHeight))
            .orElseGet(() -> Anchors.inFrontOfEyes(viewerEyes, defaultCodeDistance, codeHeight));
    }
}
