package br.com.muxe.minigame.engine;

import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.minigame.engine.arena.ArenaPool;
import br.com.muxe.minigame.engine.arena.ArenaState;
import br.com.muxe.minigame.engine.match.MatchService;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MinigameEngine {

    private final String game;
    private final Map<String, GameMode> modes;
    private final MatchService matches = new MatchService();
    private final ArenaPool arenas = new ArenaPool();

    public MinigameEngine(String game, List<GameMode> modes, MetricRegistry metrics) {
        this.game = game;
        this.modes = modes.stream().collect(Collectors.toUnmodifiableMap(GameMode::id, Function.identity()));
        Tags tags = Tags.of("game", game);
        metrics.gauge("muxe_matches_active", tags, () -> matches.active().size());
        metrics.gauge("muxe_arenas_available", tags, () -> arenas.count(ArenaState.AVAILABLE));
        metrics.gauge("muxe_arenas_disabled", tags, () -> arenas.count(ArenaState.DISABLED));
    }

    public String game() {
        return game;
    }

    public Map<String, GameMode> modes() {
        return modes;
    }

    public MatchService matches() {
        return matches;
    }

    public ArenaPool arenas() {
        return arenas;
    }
}
