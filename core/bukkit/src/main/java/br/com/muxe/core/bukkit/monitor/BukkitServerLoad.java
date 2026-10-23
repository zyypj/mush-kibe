package br.com.muxe.core.bukkit.monitor;

import br.com.muxe.core.common.server.heartbeat.ServerLoad;
import java.util.concurrent.atomic.AtomicInteger;

public final class BukkitServerLoad implements ServerLoad {

    private final AtomicInteger onlinePlayers = new AtomicInteger();
    private final int maxPlayers;

    public BukkitServerLoad(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    void sample(int online) {
        onlinePlayers.set(online);
    }

    @Override
    public int onlinePlayers() {
        return onlinePlayers.get();
    }

    @Override
    public int maxPlayers() {
        return maxPlayers;
    }
}
