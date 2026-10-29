package br.com.muxe.core.common.permission.sync;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.bus.Subscription;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.permission.PermissionsChangedPacket;
import br.com.muxe.core.common.permission.application.command.AddUserNodeCommand;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesCommand;
import br.com.muxe.core.common.permission.application.command.RemoveUserNodeCommand;
import br.com.muxe.core.common.permission.application.command.SelectMedalCommand;
import br.com.muxe.core.common.permission.application.command.SelectTagCommand;
import br.com.muxe.core.common.permission.application.query.FindPermissionUserQuery;
import br.com.muxe.core.common.permission.application.query.LoadPermissionDataQuery;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PermissionSync {

    private final CommandBus commands;
    private final QueryBus queries;
    private final PacketBus packets;
    private final Executor callbacks;
    private final Logger logger;
    private final Map<UUID, PermissionUser> users = new ConcurrentHashMap<>();
    private final List<Consumer<PermissionData>> dataListeners = new CopyOnWriteArrayList<>();
    private final List<BiConsumer<UUID, PermissionUser>> userListeners = new CopyOnWriteArrayList<>();
    private volatile PermissionData data = PermissionData.EMPTY;
    private Subscription subscription;

    public PermissionSync(CommandBus commands, QueryBus queries, PacketBus packets, Executor callbacks, Logger logger) {
        this.commands = commands;
        this.queries = queries;
        this.packets = packets;
        this.callbacks = callbacks;
        this.logger = logger;
    }

    public CompletableFuture<Void> start() {
        subscription = packets.subscribe(PermissionsChangedPacket.TYPE, context -> {
            Optional<UUID> user = context.packet().user();
            if (user.isEmpty()) {
                reloadData();
            } else if (users.containsKey(user.get())) {
                load(user.get());
            }
        });
        return reloadData();
    }

    public void stop() {
        if (subscription != null) {
            subscription.cancel();
        }
        dataListeners.clear();
        userListeners.clear();
        users.clear();
    }

    public PermissionData data() {
        return data;
    }

    public Optional<PermissionUser> cached(UUID player) {
        return Optional.ofNullable(users.get(player));
    }

    public void onDataChange(Consumer<PermissionData> listener) {
        dataListeners.add(listener);
    }

    public void onUserChange(BiConsumer<UUID, PermissionUser> listener) {
        userListeners.add(listener);
    }

    public CompletableFuture<PermissionUser> load(UUID player) {
        return queries.ask(new FindPermissionUserQuery(player)).thenApplyAsync(user -> {
            users.put(player, user);
            userListeners.forEach(listener -> listener.accept(player, user));
            return user;
        }, callbacks);
    }

    public CompletableFuture<PermissionUser> fetch(UUID player) {
        return queries.ask(new FindPermissionUserQuery(player));
    }

    public void forget(UUID player) {
        users.remove(player);
    }

    public CompletableFuture<Void> reloadData() {
        return queries.ask(new LoadPermissionDataQuery())
            .thenAcceptAsync(loaded -> {
                data = loaded;
                dataListeners.forEach(listener -> listener.accept(loaded));
            }, callbacks)
            .exceptionally(error -> {
                logger.log(Level.WARNING, "Could not load the groups, tags and tracks", error);
                return null;
            });
    }

    public CompletableFuture<Result<Void>> apply(String actor, List<PermissionChange> changes) {
        return commands.dispatch(new ApplyPermissionChangesCommand(actor, changes)).thenCompose(result -> {
            if (result.isFailure()) {
                return CompletableFuture.completedFuture(result);
            }
            publish(PermissionsChangedPacket.everything());
            changes.stream()
                .filter(PermissionChange.PutUser.class::isInstance)
                .map(change -> ((PermissionChange.PutUser) change).user().id())
                .distinct()
                .forEach(user -> publish(PermissionsChangedPacket.of(user)));
            return reloadData().thenApply(ignored -> result);
        });
    }

    public CompletableFuture<Result<PermissionUser>> addNode(String actor, UUID player, PermissionNode node) {
        return commands.dispatch(new AddUserNodeCommand(actor, player, node)).thenApply(result -> afterUserChange(player, result));
    }

    public CompletableFuture<Result<PermissionUser>> removeNode(String actor, UUID player, String key, Contexts contexts) {
        return commands.dispatch(new RemoveUserNodeCommand(actor, player, key, contexts)).thenApply(result -> afterUserChange(player, result));
    }

    public CompletableFuture<Result<Void>> selectTag(UUID player, Optional<String> tag) {
        return commands.dispatch(new SelectTagCommand(player, tag)).thenApply(result -> afterUserChange(player, result));
    }

    public CompletableFuture<Result<Void>> selectMedal(UUID player, Optional<String> medal) {
        return commands.dispatch(new SelectMedalCommand(player, medal)).thenApply(result -> afterUserChange(player, result));
    }

    private <T> Result<T> afterUserChange(UUID player, Result<T> result) {
        if (result.isSuccess()) {
            publish(PermissionsChangedPacket.of(player));
            if (users.containsKey(player)) {
                load(player);
            }
        }
        return result;
    }

    private void publish(PermissionsChangedPacket packet) {
        packets.publish(PacketTarget.broadcast(), packet).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not tell the network about a permission change", error);
            return null;
        });
    }
}
