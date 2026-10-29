package br.com.muxe.core.bukkit.network;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;

public final class GameConnector {

    private final ServerDirectory servers;
    private final NetworkGateway network;
    private final Executor mainThread;
    private final MessageCatalog messages;

    public GameConnector(ServerDirectory servers, NetworkGateway network, Executor mainThread, MessageCatalog messages) {
        this.servers = servers;
        this.network = network;
        this.mainThread = mainThread;
        this.messages = messages;
    }

    public int players(String group) {
        if (group.isEmpty()) {
            return 0;
        }
        return servers.aliveInGroup(group).stream().mapToInt(ServerStatus::onlinePlayers).sum();
    }

    public void connect(Player player, String group, String name) {
        if (servers.leastLoaded(group).isEmpty()) {
            player.sendMessage(messages.get("game-unavailable", "name", name));
            return;
        }
        player.sendMessage(messages.get("game-connecting", "name", name));
        network.connectToGroup(player.getUniqueId(), group).exceptionally(error -> {
            mainThread.execute(() -> player.sendMessage(messages.get("connect-failed")));
            return null;
        });
    }
}
