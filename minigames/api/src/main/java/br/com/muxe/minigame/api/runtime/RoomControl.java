package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoomControl {

    RoomControl STANDARD = new RoomControl() {
    };

    default boolean custom() {
        return false;
    }

    default Optional<RoomOptions> options() {
        return Optional.empty();
    }

    default List<HotbarItem> waitingItems(UUID player, List<HotbarItem> standard) {
        return standard;
    }

    default List<List<UUID>> parties(List<List<UUID>> standard) {
        return standard;
    }

    default Map<UUID, TeamColor> preferences(Map<UUID, TeamColor> standard) {
        return standard;
    }

    default Set<UUID> spectators() {
        return Set.of();
    }

    default boolean ranked() {
        return true;
    }

    default boolean restartsAtEnd() {
        return false;
    }

    default void restart() {
    }

    default Optional<HotbarItem> menuItem(UUID player) {
        return Optional.empty();
    }

    default void openMenu(UUID player) {
    }
}
