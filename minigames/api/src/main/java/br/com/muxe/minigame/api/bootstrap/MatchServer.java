package br.com.muxe.minigame.api.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.match.MatchReturns;
import br.com.muxe.minigame.api.bukkit.BukkitStage;
import br.com.muxe.minigame.api.bukkit.MatchTicker;
import br.com.muxe.minigame.api.bukkit.MatchWorldCells;
import br.com.muxe.minigame.api.command.GlobalChatCommand;
import br.com.muxe.minigame.api.customroom.bukkit.CustomRoomServer;
import br.com.muxe.minigame.api.listener.GameHandlers;
import br.com.muxe.minigame.api.listener.MatchListener;
import br.com.muxe.minigame.api.match.MatchArena;
import br.com.muxe.minigame.api.matchmaking.MatchConnector;
import br.com.muxe.minigame.api.matchmaking.ScheduledJoins;
import br.com.muxe.minigame.api.network.MatchPackets;
import br.com.muxe.minigame.api.network.RoomPublisher;
import br.com.muxe.minigame.api.rescue.bukkit.StrandedRescue;
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
import br.com.muxe.minigame.engine.mode.ModeNames;
import br.com.muxe.minigame.engine.world.server.MatchWorld;
import br.com.muxe.minigame.engine.world.server.MuxeWorlds;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
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
    private final StrandedRescue rescue;
    private final ModeNames modeNames;

    private MatchServer(JavaPlugin plugin, MuxeCore core, MessageCatalog messages, Commands commands, MatchPackets packets, MatchWorld world,
                        MatchService<A, S> service, RoomPublisher publisher, MatchTicker ticker, StrandedRescue rescue, ModeNames modeNames) {
        this.modeNames = modeNames;
        this.plugin = plugin;
        this.core = core;
        this.messages = messages;
        this.commands = commands;
        this.packets = packets;
        this.world = world;
        this.service = service;
        this.publisher = publisher;
        this.ticker = ticker;
        this.rescue = rescue;
    }

    static <A extends MatchArena, S extends MatchState> MatchServer<A, S> start(JavaPlugin plugin, MuxeCore core, List<GameMode> modes, MessageCatalog messages,
                                                                               Commands commands, MatchPackets packets, ArenaStore arenas, MuxeWorlds worlds,
                                                                               EndTitles endTitles, MinigameEngine engine, Minigame<A, S> minigame,
                                                                               Localized<MatchSettings> localized, MatchEvents events, ModeNames modeNames) {
        MatchSettings settings = localized.standard();
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
        MatchEnvironment environment = new MatchEnvironment(localized, minigame.rules(), messages, core.clock(), ThreadLocalRandom.current(), events,
            minigame.teamPreferences(), core.parties()::groupsOf, mode -> settings.knockback().profile(mode, core.knockback().profiles()));
        world.start();
        MatchService<A, S> service = new MatchService<>(minigame, modes, catalog, new MatchWorldCells(world),
            match -> new BukkitStage(core, world.world(), match.cell(), health, endTitles, returns, plugin.getLogger()),
            environment, (id, text) -> Optional.ofNullable(Bukkit.getPlayer(id)).ifPresent(player -> player.sendMessage(text)), plugin.getLogger());
        core.nameTags().override(service::nameTag);
        core.languages().onChange(player -> service.of(player.getUniqueId()).ifPresent(runtime -> runtime.refreshLanguage(player.getUniqueId())));
        core.parties().onGather((leader, members) -> {
            List<UUID> accepted = service.gather(leader, members);
            accepted.forEach(member -> Optional.ofNullable(Bukkit.getPlayer(member)).ifPresent(player -> arriveAgain(service, player)));
            return accepted;
        });
        service.openRoomsOnlyWhen(mode -> core.scheduledGames().allowsNewRoom(game, mode));
        MatchTicker ticker = MatchTicker.start(plugin, service);
        StrandedRescue rescue = StrandedRescue.start(plugin, core, service, settings.lobbyGroup());
        engine.track(service::activeMatches, () -> catalog.all().size());
        RoomPublisher publisher = new RoomPublisher(plugin, core, service, packets, List.of(settings.lobbyGroup(), game));
        publisher.onPublish(packet -> ScheduledJoins.waiting(core.scheduledGames(), game, packet.rooms())
            .forEach(id -> CompletableFuture.runAsync(() -> core.scheduledGames().roomsOpen(id), core.async())));
        publisher.start();
        return new MatchServer<>(plugin, core, messages, commands, packets, world, service, publisher, ticker, rescue, modeNames);
    }

    public MatchService<A, S> service() {
        return service;
    }

    public RoomPublisher publisher() {
        return publisher;
    }

    public MatchConnector playAgainConnector() {
        return playAgain(modeNames);
    }

    public MatchConnector playAgainConnector(Function<String, String> names) {
        return playAgain(ModeNames.plain(names));
    }

    private MatchConnector playAgain(ModeNames names) {
        MatchConnector connector = new MatchConnector(core, packets, messages, names, service::mode,
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

    public CustomRoomServer<S> customRooms(Supplier<String> schema) {
        CustomRoomServer<S> server = new CustomRoomServer<>(plugin, core, service.minigame().id(), service, schema,
            service.environment().settings().lobbyGroup(), this::moveToReserved);
        server.start(commands);
        return server;
    }

    public void listen(GameHandlers<S> handlers) {
        plugin.getServer().getPluginManager().registerEvents(new MatchListener<>(plugin, core, service, handlers, rescue), plugin);
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
        rescue.stop();
        service.stop();
        core.nameTags().clearOverride();
        world.stop();
    }
}
