package br.com.muxe.minigame.api.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.minigame.api.match.MatchArena;
import br.com.muxe.minigame.api.matchmaking.MatchConnector;
import br.com.muxe.minigame.api.network.MatchPackets;
import br.com.muxe.minigame.api.runtime.MatchEvents;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.runtime.Minigame;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.api.view.EndTitles;
import br.com.muxe.minigame.engine.MinigameEngine;
import br.com.muxe.minigame.engine.arena.ArenaSavedPacket;
import br.com.muxe.minigame.engine.arena.ArenaStore;
import br.com.muxe.minigame.engine.arena.DatabaseArenaStore;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.SetupSchema;
import br.com.muxe.minigame.engine.setup.session.SetupCommand;
import br.com.muxe.minigame.engine.setup.session.SetupService;
import br.com.muxe.minigame.engine.world.server.MuxeWorlds;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinigameBootstrap {

    private final JavaPlugin plugin;
    private final MuxeCore core;
    private final String game;
    private final List<GameMode> modes;
    private final MessageCatalog messages;
    private final Commands commands;
    private final MinigameEngine engine;
    private final MatchPackets packets;
    private final ArenaStore arenas;
    private final MuxeWorlds worlds;
    private final EndTitles endTitles;
    private SetupService setup;
    private MatchServer<?, ?> server;

    public MinigameBootstrap(JavaPlugin plugin, MuxeCore core, String game, List<GameMode> modes, MessageCatalog messages, Commands commands,
                             ConfigurationSection config) {
        this.plugin = plugin;
        this.core = core;
        this.game = game;
        this.modes = List.copyOf(modes);
        this.messages = messages;
        this.commands = commands;
        this.engine = new MinigameEngine(game, modes, core.metrics());
        this.packets = new MatchPackets(game);
        ArenaSavedPacket.register(core.packetRegistry());
        this.arenas = new DatabaseArenaStore(game, core.commands(), core.queries());
        this.worlds = new MuxeWorlds(plugin);
        this.endTitles = EndTitles.from(plugin, config.getConfigurationSection("end-titles"));
        worlds.deleteLeftovers();
    }

    public boolean isLobby() {
        return core.identity().kind() == ServerKind.LOBBY;
    }

    public MinigameEngine engine() {
        return engine;
    }

    public EndTitles endTitles() {
        return endTitles;
    }

    public MatchPackets packets() {
        return packets;
    }

    public SetupService setup(SetupSchema schema) {
        setup = new SetupService(plugin, schema, messages, worlds, arenas);
        setup.onEnter(player -> core.buildMode().enter(player));
        setup.onLeave(player -> core.buildMode().leave(player));
        setup.onSaved(name -> core.packets().publish(PacketTarget.group(game), new ArenaSavedPacket(game, name)).exceptionally(error -> {
            plugin.getLogger().log(Level.WARNING, "Could not tell the " + game + " servers that arena " + name + " was saved", error);
            return null;
        }));
        setup.start();
        commands.register(new SetupCommand(setup));
        return setup;
    }

    public MatchConnector connector(Function<String, String> modeNames) {
        MatchConnector connector = new MatchConnector(core, packets, messages, modeNames, id -> modes.stream().filter(mode -> mode.id().equals(id)).findFirst());
        connector.start();
        return connector;
    }

    public <A extends MatchArena, S extends MatchState> MatchServer<A, S> matches(Minigame<A, S> minigame, MatchSettings settings, MatchEvents events) {
        MatchServer<A, S> started = MatchServer.start(plugin, core, modes, messages, commands, packets, arenas, worlds, endTitles, engine, minigame, settings, events);
        server = started;
        return started;
    }

    public Optional<MatchServer<?, ?>> server() {
        return Optional.ofNullable(server);
    }

    public void stopSetup() {
        if (setup != null) {
            setup.stop();
        }
    }

    public void stopMatches() {
        if (server != null) {
            server.stop();
        }
    }
}
