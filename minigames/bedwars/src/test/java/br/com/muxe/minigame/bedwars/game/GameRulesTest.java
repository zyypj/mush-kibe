package br.com.muxe.minigame.bedwars.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.bedwars.game.TeamStatus.Kind;
import br.com.muxe.minigame.bedwars.game.Timeline.Event;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameRulesTest {

    private static final Instant T0 = Instant.parse("2026-10-30T18:00:00Z");
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);

    @Test
    void theScoreboardShowsGreenWhileSomeoneIsOnlineAndYellowWhenTheWholeTeamLeft() {
        assertThat(TeamStatus.of(true, 2, 1, true).kind()).isEqualTo(Kind.BED_ONLINE);
        assertThat(TeamStatus.of(true, 2, 0, true).kind()).isEqualTo(Kind.BED_OFFLINE);
        assertThat(TeamStatus.of(false, 2, 2, false)).isEqualTo(new TeamStatus(Kind.NO_BED, 2));
        assertThat(TeamStatus.of(false, 0, 0, false).isEliminated()).isTrue();
        assertThat(TeamStatus.of(true, 0, 0, false).isEliminated()).isTrue();
    }

    @Test
    void theKillGoesToTheLastHitWithinFifteenSeconds() {
        KillCredit credit = new KillCredit();
        credit.hit(B, A, T0);
        credit.hit(B, C, T0.plusSeconds(3));

        assertThat(credit.killer(B, T0.plusSeconds(10), false, Optional.empty())).contains(C);
        assertThat(credit.killer(B, T0.plusSeconds(19), false, Optional.empty())).isEmpty();
    }

    @Test
    void aFinalKillWithoutAHitterGoesToWhoeverBrokeTheBed() {
        KillCredit credit = new KillCredit(Duration.ofSeconds(15));

        assertThat(credit.killer(B, T0, true, Optional.of(A))).contains(A);
        assertThat(credit.killer(B, T0, false, Optional.of(A))).isEmpty();
        assertThat(credit.killer(B, T0, true, Optional.of(B))).isEmpty();
    }

    @Test
    void hittingYourselfNeverCounts() {
        KillCredit credit = new KillCredit();
        credit.hit(A, A, T0);

        assertThat(credit.lastHitter(A, T0)).isEmpty();
    }

    @Test
    void theTimelineNamesTheNextEventAndWhenItHappens() {
        Timeline timeline = new Timeline(List.of(
            new Event(Timeline.Kind.EMERALD_TIER, 2, 720, "Esmeralda II"),
            new Event(Timeline.Kind.DIAMOND_TIER, 2, 360, "Diamante II")
        ));

        assertThat(timeline.next(109).orElseThrow().event().name()).isEqualTo("Diamante II");
        assertThat(timeline.next(109).orElseThrow().clock()).isEqualTo("4:11");
        assertThat(timeline.next(360).orElseThrow().event().name()).isEqualTo("Esmeralda II");
        assertThat(timeline.dueAt(720)).extracting(Event::name).containsExactly("Esmeralda II");
        assertThat(timeline.next(720)).isEmpty();
    }

    @Test
    void levelsCostFiveThousandUntilNineThenFiveThousandMoreEveryTenLevels() {
        assertThat(Levels.costOf(1)).isEqualTo(5_000);
        assertThat(Levels.costOf(9)).isEqualTo(5_000);
        assertThat(Levels.costOf(10)).isEqualTo(10_000);
        assertThat(Levels.costOf(19)).isEqualTo(10_000);
        assertThat(Levels.costOf(20)).isEqualTo(15_000);
        assertThat(Levels.progress(0)).isEqualTo(new Levels.Progress(0, 0, 5_000));
        assertThat(Levels.progress(0).compactNeeded()).isEqualTo("5k");
        assertThat(Levels.progress(45_000 + 250)).isEqualTo(new Levels.Progress(9, 250, 10_000));
        assertThat(Levels.progress(55_000)).isEqualTo(new Levels.Progress(10, 0, 10_000));
        assertThatThrownBy(() -> Levels.costOf(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void endOfMatchXpAddsMinutesTeammatesAndTheWin() {
        XpRewards rewards = new XpRewards(10, 5, 250, 100, 12, 60);

        assertThat(rewards.endOfMatch(3, 2, true)).isEqualTo(30 + 10 + 250);
        assertThat(rewards.endOfMatch(3, 2, false)).isEqualTo(40);
    }

    @Test
    void theTopThreeAreSortedByScoreAndTiesKeepTheJoinOrder() {
        UUID d = new UUID(0, 4);

        List<Ranking.Entry> top = Ranking.top(Map.of(A, 3, B, 3, C, 1, d, 0), List.of(B, A, C, d));

        assertThat(top).containsExactly(new Ranking.Entry(B, 3), new Ranking.Entry(A, 3), new Ranking.Entry(C, 1));
    }
}
