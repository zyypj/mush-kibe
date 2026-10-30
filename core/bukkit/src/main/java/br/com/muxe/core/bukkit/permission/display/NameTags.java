package br.com.muxe.core.bukkit.permission.display;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.entity.Player;

public final class NameTags {

    private static final Function<UUID, Optional<NameTag>> NONE = player -> Optional.empty();

    private volatile Function<UUID, Optional<NameTag>> override = NONE;
    private volatile Consumer<Player> refresher = player -> {
    };

    public void override(Function<UUID, Optional<NameTag>> override) {
        this.override = Objects.requireNonNull(override, "override");
    }

    public void clearOverride() {
        this.override = NONE;
    }

    public Optional<NameTag> overrideFor(UUID player) {
        return override.apply(player);
    }

    public NameTag resolve(UUID player, Function<UUID, NameTag> rank) {
        return overrideFor(player).orElseGet(() -> rank.apply(player));
    }

    public void refresh(Player player) {
        refresher.accept(player);
    }

    public void attach(Consumer<Player> refresher) {
        this.refresher = Objects.requireNonNull(refresher, "refresher");
    }
}
