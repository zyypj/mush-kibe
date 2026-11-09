package br.com.muxe.core.proxy.party;

import br.com.muxe.core.common.party.domain.PartyPlayer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public final class PartyConnectionListener {

    private final PartyCoordinator parties;

    public PartyConnectionListener(PartyCoordinator parties) {
        this.parties = parties;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        parties.connected(new PartyPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getUsername()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        parties.disconnected(event.getPlayer().getUniqueId());
    }
}
