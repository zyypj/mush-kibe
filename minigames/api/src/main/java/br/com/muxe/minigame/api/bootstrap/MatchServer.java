package br.com.muxe.minigame.api.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.match.MatchReturns;
import br.com.muxe.minigame.api.bukkit.BukkitStage;
import br.com.muxe.minigame.api.bukkit.MatchTicker;
import br.com.muxe.minigame.api.bukkit.MatchWorldCells;
import br.com.muxe.minigame.api.command.GlobalChatCommand;
import br.com.muxe.minigame.api.listener.GameHandlers;
import br.com.muxe.minigame.api.listener.MatchListener;
import br.com.muxe.minigame.api.match.MatchArena;
import br.com.muxe.minigame.api.matchmaking.MatchConnector;
import br.com.muxe.minigame.api.network.MatchPackets;
import br.com.muxe.minigame.api.network.RoomPublisher;
import br.com.muxe.minigame.api.runtime.MatchEnvironment;
import br.com.muxe.minigame.api.runtime.MatchEvents;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.runtime.Minigame;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.api.view.EndTitles;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.arena.ArenaSavedPacket;
import br.com.muxe.minigame.engine.arena.ArenaStore;
import br.com.muxe.minigame.engine.match.HealthDisplay;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.world.server.MatchWorld;
import br.com.muxe.minigame.engine.world.server.MuxeWorlds;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MatchServer<A extends MatchArena, S extends MatchState> {

    private final JavaPlugin plugin;
    private final MuxeCore core;
    private final MessageCatalog messages;
    private final Commands commands;
    private final MatchPackets packets;
    private final MatchWorld world;
    private final MatchService<A, S> service;
    private final RoomPublisher publisher;
    private final MatchTicker ticker;

    private MatchServer(JavaPlugin plugin, MuxeCore core, MessageCatalog messages, Commands commands, MatchPackets packets, MatchWorld world,
                        MatchService<A, S> service, RoomPublisher publisher, MatchTicker ticker) {
        this.plugin = plugin;
        this.core = core;
        this.messages = messages;
        this.commands = commands;
        this.packets = packets;
        this.world = world;
        this.service = service;
        this.publisher = publisher;
        this.ticker = ticker;
    }

    static <A extends MatchArena, S extends MatchState> MatchServer<A, S> start(JavaPlugin plugin, MuxeCore core, List<GameMode> modes, MessageCatalog messages,
                                                                               Commands commands, MatchPackets packets, ArenaStore arenas, MuxeWorlds worlds,
                                                                               EndTitles endTitles, MinigameEngine engine, Minigame<A, S> minigame,
                                                                               MatchSettings settings, MatchEvents events) {
        String game = minigame.id();
        MatchWorld world = new MatchWorld(plugin, worlds, game);
        ArenaCatalog catalog = new ArenaCatalog(arenas, world::fits, plugin.getLogger());
        CompletableFuture.runAsync(() -> {
            try {
                plugin.getLogger().info("Loaded " + catalog.loadAll() + " ready arenas");
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not load the arenas", exception);
            }
        }, core.async());
        core.packets().subscribe(ArenaSavedPacket.TYPE, context -> {
            if (game.equals(context.packet().game())) {
                CompletableFuture.runAsync(() -> catalog.reload(context.packet().name()).ifPresent(loaded ->
                    plugin.getLogger().info("Arena " + loaded.name() + " updated")), core.async());
            }
        });
        HealthDisplay health = new HealthDisplay(messages.get("health-below-name"));
        MatchReturns returns = new MatchReturns(core.store());
        MatchEnvironment environment = new MatchEnvironment(settings, minigame.rules(), messages, core.clock(), ThreadLocalRandom.current(), events,
            minigame.teamPreferences(), core.parties()::groupsOf);
        world.start();
        MatchService<A, S> service = new MatchService<>(minigame, modes, catalog, new MatchWorldCells(world),
            match -> new BukkitStage(core, world.world(), match.cell(), health, endTitles, returns, plugin.getLogger()),
            environment, (id, text) -> Optional.ofNullable(Bukkit.getPlayer(id)).ifPresent(player -> player.sendMessage(text)), plugin.getLogger());
        core.nameTags().override(service::nameTag);
        core.parties().onGather((leader, members) -> {
            List<UUID> accepted = service.gather(leader, members);
            accepted.forEach(member -> Optional.ofNullable(Bukkit.getPlayer(member)).ifPresent(player -> arriveAgain(service, player)));
            return accepted;
        });
        MatchTicker ticker = MatchTicker.start(plugin, service);
        engine.track(service::activeMatches, () -> catalog.all().size());
        RoomPublisher publisher = new RoomPublisher(plugin, core, service, packets, List.of(settings.lobbyGroup(), game));
        publisher.start();
        return new MatchServer<>(plugin, core, messages, commands, packets, world, service, publisher, ticker);
    }

    public MatchService<A, S> service() {
        return service;
    }

    public RoomPublisher publisher() {
        return publisher;
    }

    public MatchConnector playAgainConnector(Function<String, String> modeNames) {
        MatchConnector connector = new MatchConnector(core, packets, messages, modeNames, service::mode,
            (player, request) -> service.reserve(player.getUniqueId(), request.companions(), request.mode(), request.wantedArena(), request.wantedRoom())
                .map(reserved -> {
                    moveToReserved(player);
                    request.companions().forEach(companion -> Optional.ofNullable(Bukkit.getPlayer(companion)).ifPresentOrElse(this::moveToReserved,
                        () -> core.network().connectToServer(companion, core.identity().id())));
                    return true;
                }).orElse(false));
        connector.start();
        publisher.onPublish(connector.directory()::update);
        return connector;
    }

    public void listen(GameHandlers<S> handlers) {
        plugin.getServer().getPluginManager().registerEvents(new MatchListener<>(plugin, core, service, handlers), plugin);
        commands.register(new GlobalChatCommand(service));
    }

    public void moveToReserved(Player player) {
        arriveAgain(service, player);
    }

    private static void arriveAgain(MatchService<?, ?> service, Player player) {
        service.leave(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(player);
            player.showPlayer(other);
        }
        service.arrive(player.getUniqueId(), player.getName());
    }

    void stop() {
        publisher.stop();
        ticker.stop();
        service.stop();
        core.nameTags().clearOverride();
        world.stop();
    }
}
