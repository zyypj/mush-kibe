package br.com.muxe.minigame.bedwars.custom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.minigame.api.timeline.Timeline;
import br.com.muxe.minigame.api.timeline.Timeline.Entry;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomRulesTest {

    private static final Timeline<BedWarsEvent> BASE = RoomFixtures.SETTINGS.timeline();

    @Test
    void anOrdinaryMatchKeepsEveryDefault() {
        CustomRules rules = CustomRules.of(Optional.empty());

        assertThat(rules.custom()).isFalse();
        assertThat(rules.timeline(BASE)).isSameAs(BASE);
        assertThat(rules.island(new Rate(1, 1, 10))).isEqualTo(new Rate(1, 1, 10));
        assertThat(rules.slingshotCooldown(Duration.ofSeconds(7))).isEqualTo(Duration.ofSeconds(7));
        assertThat(rules.diamonds()).isTrue();
        assertThat(rules.dragonEgg()).isFalse();
    }

    @Test
    void generatorsSpeedUpAndDropMore() {
        CustomRules rules = rules(Map.of("generators.forge.speed", "2", "generators.diamond.speed", "1.25", "generators.diamond.amount", "3",
            "generators.emerald.enabled", "false"));

        assertThat(rules.island(new Rate(1, 2, 10))).isEqualTo(new Rate(0.5, 2, 10));
        Rate diamond = rules.diamond(new Rate(30, 1, 4));
        assertThat(diamond.delaySeconds()).isEqualTo(24);
        assertThat(diamond.amount()).isEqualTo(3);
        assertThat(diamond.spawnLimit()).isEqualTo(12);
        assertThat(rules.emeralds()).isFalse();
    }

    @Test
    void eventsAreSpacedByTheirIntervalsAndDisabledOnesAreLeftOut() {
        CustomRules rules = rules(Map.of("events.diamond-2.seconds", "60", "events.emerald-2.enabled", "false"));

        Timeline<BedWarsEvent> timeline = rules.timeline(BASE);

        assertThat(timeline.entries()).extracting(Entry::name).doesNotContain("Esmeralda II");
        assertThat(timeline.entries().getFirst().at()).isEqualTo(60);
        assertThat(timeline.entries().get(1).at()).isEqualTo(60 + 360 + 360);
        assertThat(timeline.entries().getLast().event().kind()).isEqualTo(BedWarsEvent.Kind.GAME_END);
    }

    @Test
    void theDefaultIntervalsRebuildTheSameTimeline() {
        assertThat(CustomRules.intervals(BASE)).containsExactly(360, 360, 360, 360, 360, 600, 120);
        assertThat(rules(Map.of()).timeline(BASE).entries()).isEqualTo(BASE.entries());
    }

    @Test
    void theGameEndCannotBeDisabled() {
        Timeline<BedWarsEvent> timeline = rules(Map.of("events.game-end.enabled", "false")).timeline(BASE);

        assertThat(timeline.entries().getLast().event().kind()).isEqualTo(BedWarsEvent.Kind.GAME_END);
    }

    @Test
    void shopAndUpgradesFollowTheOptionsAndEverythingFree() {
        CustomRules rules = rules(Map.of("shop.la.price", "8", "shop.la.currency", "gold", "shop.tnt.enabled", "false", "upgrades.afiacao.1.price", "1",
            "upgrades.free", "true"));

        assertThat(rules.price("la", 4)).isEqualTo(8);
        assertThat(rules.currency("la", "iron")).isEqualTo("gold");
        assertThat(rules.sells("tnt")).isFalse();
        assertThat(rules.upgradePrice("afiacao", 1, 4)).isZero();
        assertThat(rules(Map.of("shop.free", "true")).price("la", 4)).isZero();
    }

    @Test
    void itemsAndOthersAreRead() {
        CustomRules rules = rules(Map.of("items.fireball", "fireball-fight", "items.slingshot-cooldown", "2", "others.totem", "dragon-egg", "others.creative", "true"));

        assertThat(rules.fireballFight()).isTrue();
        assertThat(rules.slingshotCooldown(Duration.ofSeconds(7))).isEqualTo(Duration.ofSeconds(2));
        assertThat(rules.dragonEgg()).isTrue();
        assertThat(rules.creative()).isTrue();
    }

    private static CustomRules rules(Map<String, String> values) {
        return CustomRules.of(Optional.of(new RoomOptions(values)));
    }
}
