package br.com.muxe.core.common.ultraplus.domain;

import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.UserGrants;
import java.time.Instant;

public final class UltraPlusGrants implements UserGrants {

    private final UltraPlusRepository profiles;

    public UltraPlusGrants(UltraPlusRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public boolean concerns(PermissionUser after) {
        return UltraPlusGrant.timed(after);
    }

    @Override
    public void changed(PermissionUser before, PermissionUser after, Instant now) {
        int days = UltraPlusGrant.addedDays(before, after, now);
        if (days > 0) {
            UltraPlusProfile profile = profiles.find(after.id()).orElseGet(() -> UltraPlusProfile.empty(after.id()));
            profiles.save(profile.plusDays(days), now);
        }
    }
}
