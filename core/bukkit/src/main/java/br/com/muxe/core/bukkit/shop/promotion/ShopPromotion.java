package br.com.muxe.core.bukkit.shop.promotion;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.Subscription;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.shop.PromotionChangedPacket;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.shop.application.command.ChangePromotionCommand;
import br.com.muxe.core.common.shop.application.query.FindPromotionQuery;
import br.com.muxe.core.common.shop.domain.Promotion;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ShopPromotion {

    private final Promotion defaults;
    private final CommandBus commands;
    private final QueryBus queries;
    private final PacketBus packets;
    private final Executor mainThread;
    private final Logger logger;
    private final List<Consumer<Promotion>> listeners = new CopyOnWriteArrayList<>();
    private volatile Promotion current;
    private Subscription subscription;

    public ShopPromotion(Promotion defaults, CommandBus commands, QueryBus queries, PacketBus packets, Executor mainThread, Logger logger) {
        this.defaults = defaults;
        this.current = defaults;
        this.commands = commands;
        this.queries = queries;
        this.packets = packets;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    public CompletableFuture<Void> start() {
        subscription = packets.subscribe(PromotionChangedPacket.TYPE, context -> reload());
        return reload();
    }

    public void stop() {
        if (subscription != null) {
            subscription.cancel();
        }
        listeners.clear();
    }

    public Promotion current() {
        return current;
    }

    public void onChange(Consumer<Promotion> listener) {
        listeners.add(listener);
        listener.accept(current);
    }

    public CompletableFuture<Result<Promotion>> change(Promotion promotion) {
        return commands.dispatch(new ChangePromotionCommand(promotion)).thenCompose(result -> {
            if (result.isFailure()) {
                return CompletableFuture.completedFuture(result);
            }
            packets.publish(PacketTarget.broadcast(), new PromotionChangedPacket(promotion.productId())).exceptionally(error -> {
                logger.log(Level.WARNING, "Could not tell the network about the new shop promotion", error);
                return null;
            });
            return CompletableFuture.runAsync(() -> apply(promotion), mainThread).thenApply(ignored -> result);
        });
    }

    public CompletableFuture<Void> reload() {
        return queries.ask(new FindPromotionQuery())
            .thenAcceptAsync(found -> apply(found.orElse(defaults)), mainThread)
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not load the shop promotion", error);
                return null;
            });
    }

    private void apply(Promotion promotion) {
        current = promotion;
        listeners.forEach(listener -> listener.accept(promotion));
    }
}
