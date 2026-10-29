package br.com.muxe.core.proxy.tablist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public final class TabListListener {

    private final Component header;
    private final Component footer;

    public TabListListener(TabListSettings settings) {
        this.header = settings.headerComponent();
        this.footer = settings.footerComponent();
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        show(event.getPlayer());
    }

    public void show(Player player) {
        player.sendPlayerListHeaderAndFooter(header, footer);
    }
}
