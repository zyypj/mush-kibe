package br.com.muxe.lobby.npc;

import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.bukkit.World;

public record GameNpc(String id, String group, String name, StoredLocation location, String skinName, Optional<NpcSkin> skin) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    public GameNpc {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("NPC ids use lowercase letters, digits, '-' and '_', got: " + id);
        }
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(skinName, "skinName");
        Objects.requireNonNull(skin, "skin");
    }

    public static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public List<String> lines(List<String> template, int players) {
        return template.stream().map(line -> line.replace("{name}", name).replace("{players}", Integer.toString(players))).toList();
    }

    public GameNpc movedTo(StoredLocation target) {
        return new GameNpc(id, group, name, target, skinName, skin);
    }

    public GameNpc named(String value) {
        return new GameNpc(id, group, value, location, skinName, skin);
    }

    public GameNpc wearing(String newSkinName, Optional<NpcSkin> newSkin) {
        return new GameNpc(id, group, name, location, newSkinName, newSkin);
    }

    public Optional<Npc> toNpc(Function<String, World> worlds) {
        return location.resolve(worlds).map(resolved -> new Npc(id, resolved, skin));
    }
}
