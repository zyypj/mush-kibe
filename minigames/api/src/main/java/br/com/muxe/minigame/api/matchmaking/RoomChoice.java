package br.com.muxe.minigame.api.matchmaking;

import br.com.muxe.minigame.api.network.RoomInfo;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public final class RoomChoice {

    private RoomChoice() {
    }

    public static Optional<RoomInfo> pick(Collection<RoomInfo> rooms, String mode, Optional<String> arena, Optional<String> code) {
        return pick(rooms, mode, arena, code, 1);
    }

    public static Optional<RoomInfo> pick(Collection<RoomInfo> rooms, String mode, Optional<String> arena, Optional<String> code, int seats) {
        if (code.isPresent()) {
            Optional<RoomInfo> wanted = rooms.stream().filter(room -> room.code().equals(code.get()) && room.hasRoom(seats)).findFirst();
            if (wanted.isPresent()) {
                return wanted;
            }
        }
        return rooms.stream()
            .filter(room -> room.mode().equals(mode) && room.hasRoom(seats))
            .filter(room -> arena.map(room.arena()::equals).orElse(true))
            .max(Comparator.comparingInt(RoomInfo::players).thenComparing(RoomInfo::code, Comparator.reverseOrder()));
    }
}
