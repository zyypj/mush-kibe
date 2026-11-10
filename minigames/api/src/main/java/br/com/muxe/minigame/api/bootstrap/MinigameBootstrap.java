package br.com.muxe.minigame.api.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
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
import br.com.muxe.minigame.engine.mode.ModeNames;
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

    public static final String END_TITLES = "end-titles";

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
    private final Optional<LocalizedConfig> texts;
    private final ModeNames modeNames;
    private SetupService setup;
    private MatchServer<?, ?> server;

    public MinigameBootstrap(JavaPlugin plugin, MuxeCore core, String game, List<GameMode> modes, MessageCatalog messages, Commands commands,
                             ConfigurationSection config) {
        this(plugin, core, game, modes, messages, commands, Optional.empty(), ModeNames.of(modes),
            EndTitles.from(plugin, Localized.same(Optional.ofNullable(config.getConfigurationSection(END_TITLES))), core.languages()));
    }

    public MinigameBootstrap(JavaPlugin plugin, MuxeCore core, String game, List<GameMode> modes, MessageCatalog messages, Commands commands,
                             LocalizedConfig texts) {
        this(plugin, core, game, modes, messages, commands, Optional.of(texts), ModeNames.from(texts),
            EndTitles.from(plugin, texts.map(root -> Optional.ofNullable(root.getConfigurationSection(END_TITLES))), core.languages()));
    }

    private MinigameBootstrap(JavaPlugin plugin, MuxeCore core, String game, List<GameMode> modes, MessageCatalog messages, Commands commands,
                              Optional<LocalizedConfig> texts, ModeNames modeNames, EndTitles endTitles) {
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
        this.texts = texts;
        this.modeNames = modeNames;
        this.endTitles = endTitles;
        worlds.deleteLeftovers();
    }

    public boolean isLobby() {
        return core.identity().kind() == ServerKind.LOBBY;
    }

    public ModeNames modeNames() {
        return modeNames;
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

    public MatchConnector connector() {
        return connect(modeNames);
    }

    public MatchConnector connector(Function<String, String> modeNames) {
        return connect(ModeNames.plain(modeNames));
    }

    private MatchConnector connect(ModeNames names) {
        MatchConnector connector = new MatchConnector(core, packets, messages, names, id -> modes.stream().filter(mode -> mode.id().equals(id)).findFirst());
        connector.start();
        return connector;
    }

    public <A extends MatchArena, S extends MatchState> MatchServer<A, S> matches(Minigame<A, S> minigame, MatchEvents events) {
        LocalizedConfig config = texts.orElseThrow(() -> new IllegalStateException("The " + game + " bootstrap was built without a LocalizedConfig"));
        return matches(minigame, config.map(MatchSettings::from), events);
    }

    public <A extends MatchArena, S extends MatchState> MatchServer<A, S> matches(Minigame<A, S> minigame, MatchSettings settings, MatchEvents events) {
        return matches(minigame, Localized.same(settings), events);
    }

    public <A extends MatchArena, S extends MatchState> MatchServer<A, S> matches(Minigame<A, S> minigame, Localized<MatchSettings> settings, MatchEvents events) {
        MatchServer<A, S> started = MatchServer.start(plugin, core, modes, messages, commands, packets, arenas, worlds, endTitles, engine, minigame, settings, events,
            modeNames);
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
