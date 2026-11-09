package br.com.muxe.minigame.api.settings;

import br.com.muxe.core.bukkit.combat.knockback.KnockbackProfiles;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record KnockbackChoice(Optional<String> fallback, Map<String, String> modes) {

    public static final KnockbackChoice NONE = new KnockbackChoice(Optional.empty(), Map.of());

    public KnockbackChoice {
        modes = Map.copyOf(modes);
    }

    public static KnockbackChoice from(ConfigurationSection section) {
        if (section == null) {
            return NONE;
        }
        Map<String, String> modes = new LinkedHashMap<>();
        ConfigurationSection modesSection = section.getConfigurationSection("modes");
        if (modesSection != null) {
            modesSection.getKeys(false).forEach(mode -> modes.put(mode, modesSection.getString(mode)));
        }
        return new KnockbackChoice(Optional.ofNullable(section.getString("default")), modes);
    }

    public Optional<String> profileName(String mode) {
        return Optional.ofNullable(modes.get(mode)).or(() -> fallback);
    }

    public Optional<KnockbackProfile> profile(String mode, KnockbackProfiles profiles) {
        return profileName(mode).flatMap(profiles::named);
    }
}
