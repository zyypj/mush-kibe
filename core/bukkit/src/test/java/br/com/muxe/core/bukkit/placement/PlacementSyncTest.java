package br.com.muxe.core.bukkit.placement;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.placement.PlacementModule;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryPlacementRepository;
import br.com.muxe.core.common.testing.TestNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlacementSyncTest {

    private static final Position SPAWN = new Position("lobby", 7.5, 33, -9.5, 180, 0);

    private final TestNetwork network = new TestNetwork();
    private final InMemoryPlacementRepository database = new InMemoryPlacementRepository();

    @AfterEach
    void close() {
        network.close();
    }

    @Test
    void aChangeOnOneLobbyShowsUpOnEveryLobby() {
        PlacementSync lobby1 = server("lobby-1", "lobby");
        PlacementSync lobby2 = server("lobby-2", "lobby");
        PlacementSync skywars = server("skywars-1", "skywars");
        List<Placements> seenByLobby2 = new ArrayList<>();
        lobby2.onChange(seenByLobby2::add);

        RegisterPlacement register = new RegisterPlacement("lobby", "caixa", SPAWN, Optional.of(SPAWN));
        Result<?> saved = lobby1.save(register).join();

        assertThat(saved.isSuccess()).isTrue();
        assertThat(lobby1.current().registers()).containsExactly(register);
        assertThat(lobby2.current().registers()).containsExactly(register);
        assertThat(seenByLobby2.getLast().registers()).containsExactly(register);
        assertThat(skywars.current().registers()).isEmpty();
    }

    @Test
    void removingSomethingTellsTheOtherServersToo() {
        PlacementSync lobby1 = server("lobby-1", "lobby");
        PlacementSync lobby2 = server("lobby-2", "lobby");
        lobby1.save(new HologramPlacement("lobby", "oi", SPAWN, List.of("&bOi"))).join();

        Result<Void> removed = lobby1.remove(PlacementKind.HOLOGRAM, "oi").join();
        Result<Void> missing = lobby1.remove(PlacementKind.HOLOGRAM, "oi").join();

        assertThat(removed.isSuccess()).isTrue();
        assertThat(missing.isFailure()).isTrue();
        assertThat(lobby2.current().holograms()).isEmpty();
    }

    @Test
    void aServerLoadsWhatWasPlacedBeforeItStarted() {
        server("lobby-1", "lobby").save(new HologramPlacement("lobby", "oi", SPAWN, List.of("&bOi"))).join();

        PlacementSync late = server("lobby-3", "lobby");

        assertThat(late.current().hologram("oi")).isPresent();
        assertThat(late.group()).isEqualTo("lobby");
    }

    private PlacementSync server(String id, String group) {
        TestNetwork.Node node = network.node(id, group, ServerKind.LOBBY).start();
        SimpleCommandBus commands = new SimpleCommandBus(Runnable::run, List.of());
        SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
        PlacementModule.register(commands, queries, database, new DirectTransactions());
        PlacementSync sync = new PlacementSync(group, commands, queries, node.bus(), Runnable::run, Logger.getLogger("test"));
        sync.start().join();
        return sync;
    }
}
