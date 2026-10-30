package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.world.map.EditableMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;

public final class SetupSession {

    private final ArenaDraft draft;
    private final EditableMap map;
    private final World world;
    private final Map<UUID, Return> editors = new LinkedHashMap<>();

    SetupSession(ArenaDraft draft, EditableMap map, World world) {
        this.draft = draft;
        this.map = map;
        this.world = world;
    }

    public ArenaDraft draft() {
        return draft;
    }

    public World world() {
        return world;
    }

    EditableMap map() {
        return map;
    }

    public Set<UUID> editors() {
        return Collections.unmodifiableSet(editors.keySet());
    }

    void enter(UUID player, Return previous) {
        editors.putIfAbsent(player, previous);
    }

    Return leave(UUID player) {
        return editors.remove(player);
    }

    boolean isEmpty() {
        return editors.isEmpty();
    }

    record Return(Location location, GameMode gameMode, boolean allowFlight) {
    }
}
