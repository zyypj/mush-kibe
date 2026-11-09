package br.com.muxe.minigame.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.combat.knockback.KnockbackSetting;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.TestArena;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.PartyGroups;
import br.com.muxe.minigame.engine.world.cell.Cell;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchKnockbackTest {

    private static final GameMode ONE_V_ONE = new GameMode("1v1", "1v1", 2, 2, 1);
    private static final MatchRules RULES = MatchRules.defaults("mxt").respawning();
    private static final KnockbackProfile DUEL = KnockbackProfile.VANILLA.with(KnockbackSetting.HORIZONTAL, "0.75");

    private final MutableClock clock = MutableClock.startingAt("2026-11-09T20:00:00Z");
    private final FakeStage stage = new FakeStage();
    private final UUID red = UUID.nameUUIDFromBytes("red".getBytes());
    private final UUID blue = UUID.nameUUIDFromBytes("blue".getBytes());

    @Test
    void joiningGivesThePlayerTheKnockbackOfTheMode() {
        MatchRuntime<RecordingState> runtime = runtime(mode -> mode.equals("1v1") ? Optional.of(DUEL) : Optional.empty());

        join(runtime, red);

        assertThat(runtime.knockback()).contains(DUEL);
        assertThat(stage.knockbackOf(red)).contains(DUEL);
    }

    @Test
    void aModeWithoutAProfileKeepsTheServerKnockback() {
        MatchRuntime<RecordingState> runtime = runtime(KnockbackLookup.NONE);

        join(runtime, red);

        assertThat(stage.knockbackOf(red)).isEmpty();
    }

    @Test
    void leavingTheRoomDropsTheProfile() {
        MatchRuntime<RecordingState> runtime = runtime(mode -> Optional.of(DUEL));
        join(runtime, red);

        runtime.quit(red);

        assertThat(stage.knockbackOf(red)).isEmpty();
    }

    @Test
    void changingTheKnockbackReachesEveryPlayerInTheRoom() {
        MatchRuntime<RecordingState> runtime = runtime(mode -> Optional.of(KnockbackProfile.VANILLA));
        join(runtime, red);
        join(runtime, blue);

        runtime.knockback(DUEL);

        assertThat(stage.knockbackOf(red)).contains(DUEL);
        assertThat(stage.knockbackOf(blue)).contains(DUEL);
    }

    private MatchRuntime<RecordingState> runtime(KnockbackLookup lookup) {
        Match match = new Match("abc123", ONE_V_ONE, TestArena.of("1v1", TestArena.RED_BLUE), new Cell(0, 0, 0, 64), RULES.respawn());
        MatchEnvironment environment = new MatchEnvironment(RuntimeFixtures.settings(), RULES, RuntimeFixtures.MESSAGES, clock, new Random(3), MatchEvents.NONE,
            TeamPreferences.NONE, PartyGroups.NONE, lookup);
        return new MatchRuntime<>(match, stage, environment, closed -> {
        }, opened -> new RecordingState());
    }

    private void join(MatchRuntime<RecordingState> runtime, UUID id) {
        stage.present.add(id);
        runtime.join(id, id.toString());
    }
}
