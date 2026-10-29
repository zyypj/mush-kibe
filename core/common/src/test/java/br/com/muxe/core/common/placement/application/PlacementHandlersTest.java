package br.com.muxe.core.common.placement.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.application.command.RemovePlacementCommand;
import br.com.muxe.core.common.placement.application.command.RemovePlacementHandler;
import br.com.muxe.core.common.placement.application.command.SavePlacementCommand;
import br.com.muxe.core.common.placement.application.command.SavePlacementHandler;
import br.com.muxe.core.common.placement.application.query.FindPlacementsHandler;
import br.com.muxe.core.common.placement.application.query.FindPlacementsQuery;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.PlacementProblems;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryPlacementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlacementHandlersTest {

    private static final Position SPAWN = new Position("lobby", 7.5, 33, -9.5, 180, 0);

    private final InMemoryPlacementRepository repository = new InMemoryPlacementRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final SavePlacementHandler save = new SavePlacementHandler(repository, transactions);
    private final RemovePlacementHandler remove = new RemovePlacementHandler(repository, transactions);
    private final FindPlacementsHandler find = new FindPlacementsHandler(repository, transactions);

    @Test
    void placementsAreSharedByEveryServerOfTheGroup() {
        RegisterPlacement register = new RegisterPlacement("lobby", "caixa", SPAWN, Optional.of(SPAWN));
        HologramPlacement other = new HologramPlacement("skywars", "caixa", SPAWN, List.of("&bOi"));

        save.handle(new SavePlacementCommand(register));
        save.handle(new SavePlacementCommand(other));

        Placements lobby = find.handle(new FindPlacementsQuery("lobby"));
        assertThat(lobby.registers()).containsExactly(register);
        assertThat(lobby.holograms()).isEmpty();
        assertThat(transactions.operations()).containsExactly("placement.save", "placement.save", "placement.find");
    }

    @Test
    void savingAgainReplacesThePlacement() {
        RegisterPlacement register = new RegisterPlacement("lobby", "caixa", SPAWN, Optional.empty());
        Position moved = new Position("lobby", 0, 40, 0, 0, 0);

        save.handle(new SavePlacementCommand(register));
        save.handle(new SavePlacementCommand(register.movedTo(moved)));

        assertThat(find.handle(new FindPlacementsQuery("lobby")).registers()).extracting(RegisterPlacement::position).containsExactly(moved);
    }

    @Test
    void removingSomethingMissingIsAProblem() {
        save.handle(new SavePlacementCommand(new HologramPlacement("lobby", "oi", SPAWN, List.of("&bOi"))));

        Result<Void> removed = remove.handle(new RemovePlacementCommand(PlacementKind.HOLOGRAM, "lobby", "oi"));
        Result<Void> again = remove.handle(new RemovePlacementCommand(PlacementKind.HOLOGRAM, "lobby", "oi"));

        assertThat(removed.isSuccess()).isTrue();
        assertThat(again).isInstanceOfSatisfying(Result.Failure.class, failure -> assertThat(failure.problem().code()).isEqualTo(PlacementProblems.NOT_FOUND));
        assertThat(find.handle(new FindPlacementsQuery("lobby")).holograms()).isEmpty();
    }
}
