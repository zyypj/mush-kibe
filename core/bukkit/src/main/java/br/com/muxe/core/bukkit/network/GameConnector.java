package br.com.muxe.core.bukkit.network;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import java.util.Optional;
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
        return servers.gamePlayers(group);
    }

    public void connect(Player player, String group, String name) {
        Optional<String> entry = servers.entryGroup(group);
        if (entry.isEmpty()) {
            player.sendMessage(messages.get("game-unavailable", "name", name));
            return;
        }
        network.connectToGroup(player.getUniqueId(), entry.get()).exceptionally(error -> {
            mainThread.execute(() -> player.sendMessage(messages.get("connect-failed")));
            return null;
        });
    }
}
