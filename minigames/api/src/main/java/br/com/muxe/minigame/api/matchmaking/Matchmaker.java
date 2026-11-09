package br.com.muxe.minigame.api.matchmaking;

import br.com.muxe.minigame.api.network.RoomInfo;
import br.com.muxe.minigame.api.network.RoomsPacket;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Matchmaker {

    private Matchmaker() {
    }

    public static Optional<Target> plan(List<RoomsPacket> servers, String mode, Optional<String> arena) {
        return plan(servers, mode, arena, 1);
    }

    public static Optional<Target> plan(List<RoomsPacket> servers, String mode, Optional<String> arena, int seats) {
        Optional<Target> open = servers.stream()
            .flatMap(server -> RoomChoice.pick(server.rooms(), mode, arena, Optional.empty(), seats).map(room -> new Candidate(server.serverId(), room)).stream())
            .max(Comparator.comparingInt((Candidate candidate) -> candidate.room().players()).thenComparing(Candidate::serverId, Comparator.reverseOrder()))
            .map(candidate -> new Target(candidate.serverId(), candidate.room().code()));
        if (open.isPresent()) {
            return open;
        }
        return servers.stream()
            .filter(server -> server.arenas().stream().anyMatch(info -> info.mode().equals(mode) && arena.map(info.name()::equals).orElse(true)))
            .min(Comparator.comparingInt((RoomsPacket server) -> server.rooms().stream().mapToInt(RoomInfo::players).sum()).thenComparing(RoomsPacket::serverId))
            .map(server -> new Target(server.serverId(), ""));
    }

    public record Target(String serverId, String roomCode) {

        public boolean createsRoom() {
            return roomCode.isBlank();
        }
    }

    private record Candidate(String serverId, RoomInfo room) {
    }
}
