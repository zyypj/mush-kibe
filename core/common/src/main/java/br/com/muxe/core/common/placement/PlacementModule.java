package br.com.muxe.core.common.placement;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.placement.application.command.RemovePlacementCommand;
import br.com.muxe.core.common.placement.application.command.RemovePlacementHandler;
import br.com.muxe.core.common.placement.application.command.SavePlacementCommand;
import br.com.muxe.core.common.placement.application.command.SavePlacementHandler;
import br.com.muxe.core.common.placement.application.query.FindPlacementsHandler;
import br.com.muxe.core.common.placement.application.query.FindPlacementsQuery;
import br.com.muxe.core.common.placement.domain.PlacementRepository;

public final class PlacementModule {

    private PlacementModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, PlacementRepository placements, Transactions transactions) {
        commands.register(SavePlacementCommand.class, new SavePlacementHandler(placements, transactions));
        commands.register(RemovePlacementCommand.class, new RemovePlacementHandler(placements, transactions));
        queries.register(FindPlacementsQuery.class, new FindPlacementsHandler(placements, transactions));
    }
}
