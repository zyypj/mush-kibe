package br.com.muxe.core.common.permission.sync;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.permission.PermissionModule;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryPermissionRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PermissionSyncTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");

    private final TestNetwork network = new TestNetwork();
    private final InMemoryPermissionRepository database = InMemoryPermissionRepository.seeded();
    private final UUID player = UUID.randomUUID();

    @AfterEach
    void close() {
        network.close();
    }

    @Test
    void everyServerLoadsTheGroupsAndTags() {
        PermissionSync lobby = server("lobby-1", "lobby");

        assertThat(lobby.data().group("admin")).isPresent();
        assertThat(lobby.data().tags()).containsKey("natal");
    }

    @Test
    void aTagCreatedOnOneServerShowsUpOnTheOthers() {
        PermissionSync proxy = server("proxy-1", "proxy");
        PermissionSync bedwars = server("bedwars-1", "bedwars");
        Tag halloween = new Tag("halloween", "Halloween", "&6", 95, Optional.empty(), true);

        Result<Void> saved = proxy.apply("tadeu", List.of(new PermissionChange.PutTag(halloween))).join();

        assertThat(saved.isSuccess()).isTrue();
        assertThat(bedwars.data().tag("halloween")).contains(halloween);
    }

    @Test
    void aRankGivenOnOneServerReachesTheServerWhereThePlayerIs() {
        PermissionSync lobby = server("lobby-1", "lobby");
        PermissionSync auth = server("auth-1", "auth");
        List<PermissionUser> seen = new ArrayList<>();
        lobby.onUserChange((id, user) -> seen.add(user));
        lobby.load(player).join();

        auth.addNode("Console", player, PermissionNode.group("vip", Optional.empty(), NOW)).join();

        assertThat(lobby.cached(player)).hasValueSatisfying(user -> assertThat(user.nodes()).extracting(PermissionNode::key).containsExactly("group.vip"));
        assertThat(seen.getLast().nodes()).hasSize(1);
        assertThat(auth.cached(player)).isEmpty();
    }

    @Test
    void forgottenPlayersAreNotReloaded() {
        PermissionSync lobby = server("lobby-1", "lobby");
        lobby.load(player).join();
        lobby.forget(player);

        lobby.selectTag(player, Optional.of("natal")).join();

        assertThat(lobby.cached(player)).isEmpty();
        assertThat(lobby.fetch(player).join().selectedTag()).contains("natal");
    }

    private PermissionSync server(String id, String group) {
        TestNetwork.Node node = network.node(id, group, ServerKind.LOBBY).start();
        SimpleCommandBus commands = new SimpleCommandBus(Runnable::run, List.of());
        SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
        PermissionModule.register(commands, queries, database, new DirectTransactions(), new MutableClock(NOW));
        PermissionSync sync = new PermissionSync(commands, queries, node.bus(), Runnable::run, Logger.getLogger("test"));
        sync.start().join();
        return sync;
    }
}
