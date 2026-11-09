package br.com.muxe.core.proxy.party;

import br.com.muxe.core.common.party.domain.PartyPlayer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyNetwork {

    Optional<PartyPlayer> online(String name);

    Optional<String> serverOf(UUID player);

    void connect(UUID player, String serverId);

    List<String> onlineNames();
}
