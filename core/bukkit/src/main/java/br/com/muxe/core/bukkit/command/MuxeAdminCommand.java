package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.metrics.snapshot.MetricsSnapshot;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class MuxeAdminCommand implements CommandExecutor {

    public static final String PERMISSION = "muxe.admin";

    private final MuxeCore core;

    public MuxeAdminCommand(MuxeCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageCatalog messages = core.messages();
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(messages.get("no-permission"));
            return true;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("servers")) {
            listServers(sender);
            return true;
        }
        if (action.equals("find") && args.length == 2) {
            findPlayer(sender, args[1]);
            return true;
        }
        if (action.equals("metrics")) {
            showMetrics(sender);
            return true;
        }
        sender.sendMessage(messages.get("admin.usage"));
        return true;
    }

    private void listServers(CommandSender sender) {
        MessageCatalog messages = core.messages();
        List<ServerStatus> servers = core.servers().alive();
        sender.sendMessage(messages.get("admin.servers-header", "count", Integer.toString(servers.size())));
        for (ServerStatus status : servers) {
            sender.sendMessage(messages.get("admin.servers-line",
                "id", status.identity().id(),
                "group", status.identity().group(),
                "kind", status.identity().kind().name(),
                "online", Integer.toString(status.onlinePlayers()),
                "max", Integer.toString(status.maxPlayers())));
        }
    }

    private void findPlayer(CommandSender sender, String username) {
        MessageCatalog messages = core.messages();
        core.packets()
            .request(PacketTarget.proxy(), new PlayerLocateRequest(username), PlayerLocateResponse.class, core.requestTimeout())
            .whenComplete((response, error) -> core.mainThread().execute(() -> {
                if (error != null) {
                    sender.sendMessage(messages.get("admin.find-failed", "reason", rootMessage(error)));
                } else if (response.online()) {
                    sender.sendMessage(messages.get("admin.find-online", "player", response.username(), "server", response.serverId()));
                } else {
                    sender.sendMessage(messages.get("admin.find-offline", "player", response.username()));
                }
            }));
    }

    private void showMetrics(CommandSender sender) {
        MessageCatalog messages = core.messages();
        MetricsSnapshot snapshot = core.metrics().snapshot();
        sender.sendMessage(messages.get("admin.metrics",
            "published", Long.toString(snapshot.counterTotal("muxe_packets_published_total")),
            "received", Long.toString(snapshot.counterTotal("muxe_packets_received_total")),
            "duplicates", Long.toString(snapshot.counterTotal("muxe_packets_duplicates_total")),
            "rejected", Long.toString(snapshot.counterTotal("muxe_packets_rejected_total")),
            "failures", Long.toString(snapshot.counterTotal("muxe_packets_handler_failures_total")),
            "tps", String.format(Locale.ROOT, "%.2f", snapshot.gaugeTotal("muxe_server_tps"))));
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
