package br.com.muxe.core.proxy.load;

import br.com.muxe.core.common.server.heartbeat.ServerLoad;
import com.velocitypowered.api.proxy.ProxyServer;

public final class ProxyServerLoad implements ServerLoad {

    private final ProxyServer proxy;

    public ProxyServerLoad(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public int onlinePlayers() {
        return proxy.getPlayerCount();
    }

    @Override
    public int maxPlayers() {
        return proxy.getConfiguration().getShowMaxPlayers();
    }
}
