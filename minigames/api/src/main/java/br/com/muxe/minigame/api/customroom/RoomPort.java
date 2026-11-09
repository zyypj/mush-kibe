package br.com.muxe.minigame.api.customroom;

import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.util.Optional;
import java.util.UUID;

public interface RoomPort {

    Optional<PartySnapshot> party(UUID player);

    Optional<String> onlineName(UUID player);

    void pull(UUID leader, UUID player);

    void toLobby(UUID player);

    void message(UUID player, String text);
}
