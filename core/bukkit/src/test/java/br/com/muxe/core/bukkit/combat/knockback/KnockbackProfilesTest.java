package br.com.muxe.core.bukkit.combat.knockback;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.common.combat.knockback.ClickLimiter;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class KnockbackProfilesTest {

    private final KnockbackProfiles profiles = KnockbackProfiles.from(ShopFixtures.CONFIG.getConfigurationSection("knockback"));

    @Test
    void theDefaultConfigHasTheBedWarsProfileOfTheReference() {
        KnockbackProfile bedwars = profiles.named("bedwars").orElseThrow();

        assertThat(bedwars).isEqualTo(new KnockbackProfile(0.6, 0.75, 0.1, 0.5, 0.4, 2.0, 2.0, 0.6, false, true, false, true, false, ClickLimiter.NORMAL));
        assertThat(profiles.named("skywars")).contains(bedwars);
    }

    @Test
    void theDefaultConfigHasEveryDuelsProfile() {
        assertThat(profiles.named("duels-combo").orElseThrow())
            .isEqualTo(new KnockbackProfile(0.4, 0.475, 0.0, 0.38, 0.4, 1.0, 2.0, 0.6, true, true, false, false, false, ClickLimiter.NORMAL));
        assertThat(profiles.named("duels-sopa").orElseThrow().clickLimiter()).isEqualTo(ClickLimiter.LOW);
        assertThat(profiles.named("duels-no-debuff").orElseThrow().vertical()).isEqualTo(0.355);
        assertThat(profiles.named("vanilla")).contains(KnockbackProfile.VANILLA);
        assertThat(profiles.named().keySet()).hasSize(21);
    }

    @Test
    void theLimiterTicksComeFromTheConfig() {
        assertThat(profiles.ticks(ClickLimiter.OFF)).isZero();
        assertThat(profiles.ticks(ClickLimiter.NORMAL)).isEqualTo(4);
    }

    @Test
    void aProfileSectionOnlyOverridesTheKeysItHas() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("vertical", 0.5);
        yaml.set("click-limiter", "high");

        KnockbackProfile read = KnockbackProfiles.read(yaml, KnockbackProfile.VANILLA);

        assertThat(read.vertical()).isEqualTo(0.5);
        assertThat(read.horizontal()).isEqualTo(0.4);
        assertThat(read.clickLimiter()).isEqualTo(ClickLimiter.HIGH);
    }

    @Test
    void assigningKeepsOneStatePerPlayerAndClearingDropsIt() {
        KnockbackService service = new KnockbackService(profiles);
        UUID player = UUID.randomUUID();

        service.assign(player, KnockbackProfile.VANILLA);
        KnockbackService.Slot first = service.slot(player);
        service.assign(player, profiles.named("bedwars").orElseThrow());

        assertThat(service.slot(player)).isSameAs(first);
        assertThat(service.profile(player)).contains(profiles.named("bedwars").orElseThrow());
        service.clear(player);
        assertThat(service.profile(player)).isEmpty();
    }
}
