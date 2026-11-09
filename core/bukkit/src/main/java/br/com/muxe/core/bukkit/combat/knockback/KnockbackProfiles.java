package br.com.muxe.core.bukkit.combat.knockback;

import br.com.muxe.core.common.combat.knockback.ClickLimiter;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.combat.knockback.KnockbackSetting;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record KnockbackProfiles(Map<String, KnockbackProfile> named, Map<ClickLimiter, Integer> limiterTicks) {

    public static final Map<ClickLimiter, Integer> DEFAULT_LIMITER_TICKS = Map.of(ClickLimiter.OFF, 0, ClickLimiter.LOW, 2, ClickLimiter.NORMAL, 4, ClickLimiter.HIGH, 6);

    public KnockbackProfiles {
        named = Map.copyOf(named);
        Map<ClickLimiter, Integer> ticks = new EnumMap<>(DEFAULT_LIMITER_TICKS);
        ticks.putAll(limiterTicks);
        limiterTicks = Map.copyOf(ticks);
    }

    public static KnockbackProfiles from(ConfigurationSection section) {
        if (section == null) {
            return new KnockbackProfiles(Map.of(), Map.of());
        }
        Map<ClickLimiter, Integer> ticks = new EnumMap<>(ClickLimiter.class);
        ConfigurationSection limiter = section.getConfigurationSection("click-limiter-ticks");
        if (limiter != null) {
            for (ClickLimiter level : ClickLimiter.values()) {
                if (limiter.contains(level.key())) {
                    ticks.put(level, Math.max(0, limiter.getInt(level.key())));
                }
            }
        }
        Map<String, KnockbackProfile> named = new LinkedHashMap<>();
        ConfigurationSection profiles = section.getConfigurationSection("profiles");
        if (profiles != null) {
            for (String name : profiles.getKeys(false)) {
                named.put(name.toLowerCase(Locale.ROOT), read(profiles.getConfigurationSection(name), KnockbackProfile.VANILLA));
            }
        }
        return new KnockbackProfiles(named, ticks);
    }

    public static KnockbackProfile read(ConfigurationSection section, KnockbackProfile base) {
        KnockbackProfile profile = base;
        if (section == null) {
            return profile;
        }
        for (KnockbackSetting setting : KnockbackSetting.values()) {
            if (section.contains(setting.key())) {
                profile = setting.write(profile, String.valueOf(section.get(setting.key())));
            }
        }
        return profile;
    }

    public Optional<KnockbackProfile> named(String name) {
        return Optional.ofNullable(named.get(name.toLowerCase(Locale.ROOT)));
    }

    public int ticks(ClickLimiter limiter) {
        return limiterTicks.getOrDefault(limiter, 0);
    }
}
