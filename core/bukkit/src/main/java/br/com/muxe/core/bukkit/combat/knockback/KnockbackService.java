package br.com.muxe.core.bukkit.combat.knockback;

import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.combat.knockback.KnockbackState;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.util.Vector;

public final class KnockbackService {

    private final KnockbackProfiles profiles;
    private final Map<UUID, Slot> slots = new HashMap<>();

    public KnockbackService(KnockbackProfiles profiles) {
        this.profiles = profiles;
    }

    public KnockbackProfiles profiles() {
        return profiles;
    }

    public Optional<KnockbackProfile> named(String name) {
        return profiles.named(name);
    }

    public void assign(UUID player, KnockbackProfile profile) {
        Slot slot = slots.get(player);
        if (slot == null) {
            slots.put(player, new Slot(new KnockbackState(profile), new Vector()));
        } else {
            slot.state().profile(profile);
        }
    }

    public void clear(UUID player) {
        slots.remove(player);
    }

    public Optional<KnockbackProfile> profile(UUID player) {
        Slot slot = slots.get(player);
        return slot == null ? Optional.empty() : Optional.of(slot.state().profile());
    }

    Slot slot(UUID player) {
        return slots.get(player);
    }

    record Slot(KnockbackState state, Vector vector) {
    }
}
