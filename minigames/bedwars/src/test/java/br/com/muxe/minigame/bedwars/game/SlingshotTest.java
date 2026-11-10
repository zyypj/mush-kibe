package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SlingshotTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final UUID TADEU = UUID.randomUUID();

    @Test
    void theSlingshotThrowsThePlayerForwardAndUp() {
        Vec push = Slingshot.push(new Vec(-0.014919, -0.09779, 0.995095));

        assertThat(push.x()).isCloseTo(-0.023311, within(1e-6));
        assertThat(push.y()).isEqualTo(1.0);
        assertThat(push.z()).isCloseTo(1.554836, within(1e-6));
    }

    @Test
    void itCanBeUsedAgainOnlySevenSecondsLater() {
        Slingshot slingshot = new Slingshot();

        assertThat(slingshot.use(TADEU, NOW)).isTrue();
        assertThat(slingshot.use(TADEU, NOW.plusSeconds(6))).isFalse();
        assertThat(slingshot.remaining(TADEU, NOW.plusSeconds(6))).contains(Duration.ofSeconds(1));
        assertThat(slingshot.use(TADEU, NOW.plusSeconds(7))).isTrue();
    }

    @Test
    void theActionBarUnderlinesLessOfTheNameAsTheCooldownRunsOut() {
        assertThat(Slingshot.actionBar(Duration.ofMillis(7000))).isEqualTo("§c§n§lESTILINGUE:§r 7,0s");
        assertThat(Slingshot.actionBar(Duration.ofMillis(6300))).isEqualTo("§c§n§lESTILINGUE§c§l:§r 6,3s");
        assertThat(Slingshot.actionBar(Duration.ofMillis(5800))).isEqualTo("§c§n§lESTILINGUE§c§l:§r 5,8s");
        assertThat(Slingshot.actionBar(Duration.ofMillis(5700))).isEqualTo("§c§n§lESTILINGU§c§lE:§r 5,7s");
        assertThat(Slingshot.actionBar(Duration.ofMillis(4400))).isEqualTo("§c§n§lESTILIN§c§lGUE:§r 4,4s");
        assertThat(Slingshot.actionBar(Duration.ofMillis(4351))).isEqualTo("§c§n§lESTILIN§c§lGUE:§r 4,4s");
    }

    @Test
    void theActionBarShowsTheNameInTheLanguageOfThePlayer() {
        assertThat(Slingshot.actionBar("SLINGSHOT:", ".", Duration.ofMillis(7000), Slingshot.COOLDOWN)).isEqualTo("§c§n§lSLINGSHOT:§r 7.0s");
        assertThat(Slingshot.actionBar("SLINGSHOT:", ".", Duration.ofMillis(3500), Slingshot.COOLDOWN)).isEqualTo("§c§n§lSLING§c§lSHOT:§r 3.5s");
    }
}
