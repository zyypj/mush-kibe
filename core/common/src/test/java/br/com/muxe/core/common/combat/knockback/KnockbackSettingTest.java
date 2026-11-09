package br.com.muxe.core.common.combat.knockback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KnockbackSettingTest {

    @Test
    void everySettingReadsBackWhatWasWritten() {
        KnockbackProfile profile = KnockbackProfile.VANILLA;
        for (KnockbackSetting setting : KnockbackSetting.values()) {
            String value = switch (setting.kind()) {
                case DECIMAL -> "1.25";
                case TOGGLE -> String.valueOf(!Boolean.parseBoolean(setting.read(profile)));
                case CHOICE -> "high";
            };

            KnockbackProfile changed = profile.with(setting, value);

            assertThat(setting.read(changed)).as(setting.key()).isEqualTo(value);
        }
    }

    @Test
    void decimalsAcceptACommaAndKeepTheirShortForm() {
        KnockbackProfile profile = KnockbackProfile.VANILLA.with(KnockbackSetting.VERTICAL, "0,355");

        assertThat(profile.vertical()).isEqualTo(0.355);
        assertThat(KnockbackSetting.VERTICAL.read(profile)).isEqualTo("0.355");
        assertThat(KnockbackSetting.VERTICAL_FRICTION.read(profile)).isEqualTo("2.0");
    }

    @Test
    void refusesWhatIsNotANumberOrAZeroFriction() {
        assertThatThrownBy(() -> KnockbackProfile.VANILLA.with(KnockbackSetting.HORIZONTAL, "abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KnockbackProfile.VANILLA.with(KnockbackSetting.VERTICAL_FRICTION, "0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KnockbackProfile.VANILLA.with(KnockbackSetting.HORIZONTAL, "NaN")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findsSettingsByKey() {
        assertThat(KnockbackSetting.byKey("click-limiter")).contains(KnockbackSetting.CLICK_LIMITER);
        assertThat(KnockbackSetting.byKey("nothing")).isEmpty();
    }

    @Test
    void theClickLimiterCyclesThroughItsLevels() {
        assertThat(ClickLimiter.OFF.next()).isEqualTo(ClickLimiter.LOW);
        assertThat(ClickLimiter.HIGH.next()).isEqualTo(ClickLimiter.OFF);
        assertThat(ClickLimiter.parse(" Normal ")).contains(ClickLimiter.NORMAL);
    }
}
