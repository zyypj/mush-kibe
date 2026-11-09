package br.com.muxe.minigame.api.customroom;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.minigame.api.customroom.CustomRooms.RoomItems;
import br.com.muxe.minigame.api.runtime.RoomControl;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class CustomRoomControl implements RoomControl {

    private final CustomRoom room;
    private final RoomItems items;
    private final Consumer<String> restart;

    public CustomRoomControl(CustomRoom room, RoomItems items, Consumer<String> restart) {
        this.room = room;
        this.items = items;
        this.restart = restart;
    }

    public CustomRoom room() {
        return room;
    }

    @Override
    public boolean custom() {
        return true;
    }

    @Override
    public Optional<RoomOptions> options() {
        return Optional.of(room.options());
    }

    @Override
    public List<HotbarItem> waitingItems(UUID player, List<HotbarItem> standard) {
        return items.waiting(room, player, standard);
    }

    @Override
    public List<List<UUID>> parties(List<List<UUID>> standard) {
        return List.of();
    }

    @Override
    public Map<UUID, TeamColor> preferences(Map<UUID, TeamColor> standard) {
        return room.chosen();
    }

    @Override
    public Set<UUID> spectators() {
        return room.spectators();
    }

    @Override
    public boolean ranked() {
        return false;
    }

    @Override
    public boolean restartsAtEnd() {
        return true;
    }

    @Override
    public void restart() {
        restart.accept(room.code());
    }
}
