package br.com.muxe.core.common.ultraplus.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UltraPlusGrantTest {

    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void aNewThirtyDayGrantAddsOneMonth() {
        PermissionUser before = PermissionUser.empty(PLAYER);
        PermissionUser after = before.with(ultraPlus(NOW.plus(Duration.ofDays(30))));

        int days = UltraPlusGrant.addedDays(before, after, NOW);

        assertThat(days).isEqualTo(30);
        assertThat(UltraPlusProfile.empty(PLAYER).plusDays(days).months()).isEqualTo(1);
    }

    @Test
    void extendingAnActiveGrantOnlyCountsTheNewDays() {
        PermissionUser before = PermissionUser.empty(PLAYER).with(ultraPlus(NOW.plus(Duration.ofDays(10))));
        PermissionUser after = before.with(ultraPlus(NOW.plus(Duration.ofDays(40))));

        assertThat(UltraPlusGrant.addedDays(before, after, NOW)).isEqualTo(30);
    }

    @Test
    void aGrantAfterAnExpiredOneCountsFromNow() {
        PermissionUser before = new PermissionUser(PLAYER, List.of(ultraPlus(NOW.minus(Duration.ofDays(3)))), Optional.empty());
        PermissionUser after = PermissionUser.empty(PLAYER).with(ultraPlus(NOW.plus(Duration.ofDays(30))));

        assertThat(UltraPlusGrant.addedDays(before, after, NOW)).isEqualTo(30);
    }

    @Test
    void permanentGrantsRemovalsAndOtherRanksAddNothing() {
        PermissionUser permanent = PermissionUser.empty(PLAYER).with(PermissionNode.group(UltraPlusGrant.GROUP, Optional.empty(), NOW));
        PermissionUser timed = PermissionUser.empty(PLAYER).with(ultraPlus(NOW.plus(Duration.ofDays(30))));
        PermissionUser vip = PermissionUser.empty(PLAYER).with(PermissionNode.group("vip", Optional.of(NOW.plus(Duration.ofDays(30))), NOW));

        assertThat(UltraPlusGrant.addedDays(PermissionUser.empty(PLAYER), permanent, NOW)).isZero();
        assertThat(UltraPlusGrant.addedDays(permanent, timed, NOW)).isZero();
        assertThat(UltraPlusGrant.addedDays(timed, PermissionUser.empty(PLAYER), NOW)).isZero();
        assertThat(UltraPlusGrant.addedDays(PermissionUser.empty(PLAYER), vip, NOW)).isZero();
    }

    @Test
    void theGrantRecorderSumsTheDaysOfEachGrant() {
        InMemoryUltraPlusRepository profiles = new InMemoryUltraPlusRepository();
        UltraPlusGrants grants = new UltraPlusGrants(profiles);
        PermissionUser first = PermissionUser.empty(PLAYER).with(ultraPlus(NOW.plus(Duration.ofDays(30))));

        grants.changed(PermissionUser.empty(PLAYER), first, NOW);
        grants.changed(first, first.with(ultraPlus(NOW.plus(Duration.ofDays(210)))), NOW);

        assertThat(profiles.find(PLAYER).orElseThrow().grantedDays()).isEqualTo(210);
        assertThat(profiles.find(PLAYER).orElseThrow().months()).isEqualTo(7);
    }

    private static PermissionNode ultraPlus(Instant until) {
        return PermissionNode.group(UltraPlusGrant.GROUP, Optional.of(until), NOW);
    }
}
