package br.com.muxe.core.common.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankHelpersTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");
    private static final PermissionData DATA = PermissionFixtures.DATA;
    private static final Track DONATION = DATA.track("doacao").orElseThrow();
    private static final Track STAFF = DATA.track("staff").orElseThrow();

    @Test
    void lifetimesAreWrittenAsUnitsOrAsPermanent() {
        assertThat(Lifetime.parse("30d")).contains(Lifetime.of(Duration.ofDays(30)));
        assertThat(Lifetime.parse("1d12h")).contains(Lifetime.of(Duration.ofHours(36)));
        assertThat(Lifetime.parse("2w")).contains(Lifetime.of(Duration.ofDays(14)));
        assertThat(Lifetime.parse("Permanente")).contains(Lifetime.PERMANENT);
        assertThat(Lifetime.parse("vitalício")).contains(Lifetime.PERMANENT);
        assertThat(Lifetime.parse("0d")).isEmpty();
        assertThat(Lifetime.parse("30")).isEmpty();
        assertThat(Lifetime.parse("d30")).isEmpty();
        assertThat(Lifetime.parse("1d x")).isEmpty();
        assertThat(Lifetime.parse("")).isEmpty();
    }

    @Test
    void aTemporaryGrantExtendsTheTimeLeft() {
        PermissionNode running = new PermissionNode("group.vip", true, Contexts.NONE, Optional.of(NOW.plus(Duration.ofDays(10))), NOW, NOW);
        PermissionNode expired = new PermissionNode("group.vip", true, Contexts.NONE, Optional.of(NOW.minusSeconds(1)), NOW, NOW);
        Lifetime month = Lifetime.of(Duration.ofDays(30));

        assertThat(month.expiryAfter(Optional.of(running), NOW)).contains(NOW.plus(Duration.ofDays(40)));
        assertThat(month.expiryAfter(Optional.of(expired), NOW)).contains(NOW.plus(Duration.ofDays(30)));
        assertThat(month.expiryAfter(Optional.empty(), NOW)).contains(NOW.plus(Duration.ofDays(30)));
        assertThat(Lifetime.PERMANENT.expiryAfter(Optional.of(running), NOW)).isEmpty();
    }

    @Test
    void remainingTimeKeepsTheTwoBiggestUnits() {
        assertThat(RemainingTime.format(Duration.ofDays(29).plusHours(23).plusMinutes(59))).isEqualTo("29d 23h");
        assertThat(RemainingTime.format(Duration.ofDays(30))).isEqualTo("30d");
        assertThat(RemainingTime.format(Duration.ofHours(5).plusMinutes(3))).isEqualTo("5h 3m");
        assertThat(RemainingTime.format(Duration.ofSeconds(125))).isEqualTo("2m 5s");
        assertThat(RemainingTime.format(Duration.ofSeconds(9))).isEqualTo("9s");
        assertThat(RemainingTime.format(Duration.ofSeconds(-4))).isEqualTo("0s");
    }

    @Test
    void heldRanksListTheActiveGroupsGivenToThePlayerHighestFirst() {
        PermissionUser user = user(
            PermissionNode.group("vip", Optional.empty(), NOW),
            PermissionNode.group("ultra", Optional.of(NOW.plus(Duration.ofDays(3))), NOW),
            PermissionNode.group("pro", Optional.of(NOW.minusSeconds(1)), NOW),
            PermissionNode.group("membro", Optional.empty(), NOW),
            PermissionNode.permanent("muxe.tag.natal", true, NOW)
        );

        List<HeldRank> ranks = HeldRank.of(DATA, user, NOW);

        assertThat(ranks).extracting(HeldRank::display).containsExactly("&dUltra", "&aVIP");
        assertThat(ranks.getFirst().isPermanent()).isFalse();
        assertThat(ranks.getFirst().remaining(NOW)).contains(Duration.ofDays(3));
        assertThat(ranks.getLast().isPermanent()).isTrue();
        assertThat(HeldRank.defaultRank(DATA, NOW)).hasValueSatisfying(rank -> assertThat(rank.display()).isEqualTo("&7Membro"));
    }

    @Test
    void promotingWalksUpTheTrackReplacingTheCurrentRank() {
        assertThat(move(TrackMove.promote(DONATION, user(), NOW))).isEqualTo(new TrackMove(Optional.empty(), Optional.of("vip")));
        assertThat(move(TrackMove.promote(DONATION, user(PermissionNode.group("vip", Optional.empty(), NOW)), NOW)))
            .isEqualTo(new TrackMove(Optional.of("vip"), Optional.of("mvp")));
        assertThat(move(TrackMove.promote(STAFF, user(), NOW))).isEqualTo(new TrackMove(Optional.empty(), Optional.of("mod")));
        assertThat(TrackMove.promote(DONATION, user(PermissionNode.group("ultra", Optional.empty(), NOW)), NOW).isFailure()).isTrue();
    }

    @Test
    void demotingWalksDownAndNeverAddsTheDefaultGroup() {
        assertThat(move(TrackMove.demote(DONATION, user(PermissionNode.group("mvp", Optional.empty(), NOW)), NOW)))
            .isEqualTo(new TrackMove(Optional.of("mvp"), Optional.of("vip")));
        assertThat(move(TrackMove.demote(DONATION, user(PermissionNode.group("vip", Optional.empty(), NOW)), NOW)))
            .isEqualTo(new TrackMove(Optional.of("vip"), Optional.empty()));
        assertThat(move(TrackMove.demote(STAFF, user(PermissionNode.group("mod", Optional.empty(), NOW)), NOW)))
            .isEqualTo(new TrackMove(Optional.of("mod"), Optional.empty()));
        assertThat(TrackMove.demote(DONATION, user(), NOW).isFailure()).isTrue();
    }

    private static TrackMove move(Result<TrackMove> result) {
        return result.orElseThrow();
    }

    private static PermissionUser user(PermissionNode... nodes) {
        return new PermissionUser(UUID.randomUUID(), List.of(nodes), Optional.empty());
    }
}
