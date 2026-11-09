package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import java.util.Optional;

@FunctionalInterface
public interface KnockbackLookup {

    KnockbackLookup NONE = mode -> Optional.empty();

    Optional<KnockbackProfile> forMode(String mode);
}
