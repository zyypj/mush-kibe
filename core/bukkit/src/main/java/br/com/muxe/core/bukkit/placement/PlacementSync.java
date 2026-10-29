package br.com.muxe.core.bukkit.placement;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.Subscription;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.placement.PlacementsChangedPacket;
import br.com.muxe.core.common.placement.application.command.RemovePlacementCommand;
import br.com.muxe.core.common.placement.application.command.SavePlacementCommand;
import br.com.muxe.core.common.placement.application.query.FindPlacementsQuery;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlacementSync {

    private final String group;
    private final CommandBus commands;
    private final QueryBus queries;
    private final PacketBus packets;
    private final Executor mainThread;
    private final Logger logger;
    private final List<Consumer<Placements>> listeners = new CopyOnWriteArrayList<>();
    private volatile Placements current = Placements.NONE;
    private Subscription subscription;

    public PlacementSync(String group, CommandBus commands, QueryBus queries, PacketBus packets, Executor mainThread, Logger logger) {
        this.group = group;
        this.commands = commands;
        this.queries = queries;
        this.packets = packets;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    public CompletableFuture<Void> start() {
        subscription = packets.subscribe(PlacementsChangedPacket.TYPE, context -> {
            if (context.packet().group().equals(group)) {
                reload();
            }
        });
        return reload();
    }

    public void stop() {
        if (subscription != null) {
            subscription.cancel();
        }
        listeners.clear();
    }

    public String group() {
        return group;
    }

    public Placements current() {
        return current;
    }

    public void onChange(Consumer<Placements> listener) {
        listeners.add(listener);
        listener.accept(current);
    }

    public CompletableFuture<Result<Placement>> save(Placement placement) {
        return commands.dispatch(new SavePlacementCommand(placement)).thenCompose(this::afterChange);
    }

    public CompletableFuture<Result<Void>> remove(PlacementKind kind, String id) {
        return commands.dispatch(new RemovePlacementCommand(kind, group, id)).thenCompose(this::afterChange);
    }

    public CompletableFuture<Void> reload() {
        return queries.ask(new FindPlacementsQuery(group))
            .thenAcceptAsync(this::apply, mainThread)
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not load the NPCs, holograms and registers of group '" + group + "'", error);
                return null;
            });
    }

    private <T> CompletableFuture<Result<T>> afterChange(Result<T> result) {
        if (result.isFailure()) {
            return CompletableFuture.completedFuture(result);
        }
        packets.publish(PacketTarget.group(group), new PlacementsChangedPacket(group)).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not tell group '" + group + "' about a placement change", error);
            return null;
        });
        return reload().thenApply(ignored -> result);
    }

    private void apply(Placements placements) {
        current = placements;
        listeners.forEach(listener -> listener.accept(placements));
    }
}
