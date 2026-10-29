package br.com.muxe.core.proxy.motd;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import net.kyori.adventure.text.Component;

public final class MotdListener {

    private final Component description;
    private final int maxPlayers;

    public MotdListener(MotdSettings settings) {
        this.description = settings.description();
        this.maxPlayers = settings.maxPlayers();
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        event.setPing(event.getPing().asBuilder()
            .description(description)
            .maximumPlayers(maxPlayers)
            .build());
    }
}
