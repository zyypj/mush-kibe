package br.com.muxe.minigame.api.network;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomSource {

    List<RoomInfo> summaries();

    List<ArenaInfo> arenas();

    Optional<Reserved> reserve(UUID playerId, List<UUID> companions, String mode, Optional<String> arena, Optional<String> code);
}
