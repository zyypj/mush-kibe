package br.com.muxe.core.bukkit.permission;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tristate;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PermissionCacheTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final AtomicReference<PermissionData> data = new AtomicReference<>(PermissionFixtures.DATA);
    private final PermissionCache cache = new PermissionCache(data::get, "lobby", clock);
    private final UUID player = UUID.randomUUID();

    @Test
    void permissionsFollowTheServerGroupAndTheWorld() {
        PermissionNode lobbyFly = new PermissionNode("muxe.fly", true, Contexts.of(Contexts.SERVER, "lobby"), Optional.empty(), NOW, NOW);
        PermissionNode arenaBuild = new PermissionNode("muxe.build", true, Contexts.of(Contexts.WORLD, "arena"), Optional.empty(), NOW, NOW);
        PermissionNode bedwarsFly = new PermissionNode("muxe.kit", true, Contexts.of(Contexts.SERVER, "bedwars"), Optional.empty(), NOW, NOW);
        cache.put(user(lobbyFly, arenaBuild, bedwarsFly), "lobby");

        assertThat(cache.check(player, "muxe.fly")).isEqualTo(Tristate.TRUE);
        assertThat(cache.check(player, "muxe.kit")).isEqualTo(Tristate.UNDEFINED);
        assertThat(cache.check(player, "muxe.build")).isEqualTo(Tristate.UNDEFINED);

        cache.moveTo(player, "Arena");

        assertThat(cache.check(player, "muxe.build")).isEqualTo(Tristate.TRUE);
        assertThat(cache.world(player)).contains("Arena");
    }

    @Test
    void ranksGiveTheirInheritedPermissionsAndTags() {
        cache.put(user(PermissionNode.group("mod", Optional.empty(), NOW)), "lobby");

        assertThat(cache.resolved(player).orElseThrow().primaryGroup()).map(Group::id).contains("mod");
        assertThat(cache.check(player, "muxe.tag.vip")).isEqualTo(Tristate.TRUE);
        assertThat(cache.check(player, "muxe.tag.admin")).isEqualTo(Tristate.UNDEFINED);
        assertThat(cache.check(UUID.randomUUID(), "muxe.tag.vip")).isEqualTo(Tristate.UNDEFINED);
    }

    @Test
    void anExpiredRankIsDroppedOnTheNextCheck() {
        cache.put(user(PermissionNode.group("vip", Optional.of(NOW.plus(Duration.ofMinutes(5))), NOW)), "lobby");
        assertThat(cache.refreshExpired()).isEmpty();
        assertThat(cache.check(player, "group.vip")).isEqualTo(Tristate.TRUE);

        clock.advance(Duration.ofMinutes(5));

        assertThat(cache.refreshExpired()).containsExactly(player);
        assertThat(cache.check(player, "group.vip")).isEqualTo(Tristate.UNDEFINED);
        assertThat(cache.refreshExpired()).isEmpty();
    }

    @Test
    void changedGroupsReachEveryCachedPlayer() {
        cache.put(user(PermissionNode.group("vip", Optional.empty(), NOW)), "lobby");
        Map<String, Group> groups = new HashMap<>(PermissionFixtures.DATA.groups());
        Group vip = groups.get("vip");
        List<PermissionNode> nodes = new ArrayList<>(vip.nodes());
        nodes.add(PermissionNode.permanent("muxe.lobby.setspawn", true, NOW));
        groups.put("vip", vip.withNodes(nodes));
        data.set(new PermissionData(groups, PermissionFixtures.DATA.tags(), PermissionFixtures.DATA.tracks()));

        assertThat(cache.check(player, "muxe.lobby.setspawn")).isEqualTo(Tristate.UNDEFINED);
        assertThat(cache.refreshAll()).containsExactly(player);
        assertThat(cache.check(player, "muxe.lobby.setspawn")).isEqualTo(Tristate.TRUE);
    }

    @Test
    void forgottenPlayersHaveNothingCached() {
        cache.put(user(), "lobby");
        cache.remove(player);

        assertThat(cache.resolved(player)).isEmpty();
        assertThat(cache.moveTo(player, "lobby")).isEmpty();
    }

    @Test
    void worldsThatCannotBeAContextOnlyKeepTheServer() {
        assertThat(cache.contexts("a=b")).isEqualTo(Contexts.of(Contexts.SERVER, "lobby"));
        assertThat(cache.contexts("lobby")).isEqualTo(Contexts.server("lobby", "lobby"));
    }

    private PermissionUser user(PermissionNode... nodes) {
        return new PermissionUser(player, List.of(nodes), Optional.empty());
    }
}
