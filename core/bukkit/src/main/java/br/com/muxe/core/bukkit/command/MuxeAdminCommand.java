package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.nms.JoinTraffic;
import br.com.muxe.core.bukkit.nms.PlayerPing;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.metrics.snapshot.MetricsSnapshot;
import br.com.muxe.core.common.metrics.snapshot.SlowestTimers;
import br.com.muxe.core.common.network.player.PlayerLocateRequest;
import br.com.muxe.core.common.network.player.PlayerLocateResponse;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@Command(name = "muxe", description = "Network administration.", usage = "admin.usage")
@Permission(MuxeAdminCommand.PERMISSION)
public final class MuxeAdminCommand {

    public static final String PERMISSION = "muxe.admin";
    public static final int LATENCY_LINES = 12;
    public static final int JOIN_PACKET_KINDS = 6;

    private final MuxeCore core;
    private final Optional<JoinTraffic> joins;
    private Optional<PlayerPing> pingReader;

    public MuxeAdminCommand(MuxeCore core) {
        this(core, Optional.empty());
    }

    public MuxeAdminCommand(MuxeCore core, Optional<JoinTraffic> joins) {
        this.core = core;
        this.joins = joins;
    }

    @Subcommand("servers")
    public void listServers(CommandSender sender) {
        MessageCatalog messages = core.messages();
        List<ServerStatus> servers = core.servers().alive();
        sender.sendMessage(messages.get(sender, "admin.servers-header", "count", Integer.toString(servers.size())));
        for (ServerStatus status : servers) {
            sender.sendMessage(messages.get(sender, "admin.servers-line",
                "id", status.identity().id(),
                "group", status.identity().group(),
                "kind", status.identity().kind().name(),
                "online", Integer.toString(status.onlinePlayers()),
                "max", Integer.toString(status.maxPlayers())));
        }
    }

    @Subcommand("find")
    public void findPlayer(CommandSender sender, @Arg("jogador") @Suggest(provider = SuggestionRegistry.PLAYERS) String username) {
        MessageCatalog messages = core.messages();
        core.packets()
            .request(PacketTarget.proxy(), new PlayerLocateRequest(username), PlayerLocateResponse.class, core.requestTimeout())
            .whenComplete((response, error) -> core.mainThread().execute(() -> {
                if (error != null) {
                    sender.sendMessage(messages.get(sender, "admin.find-failed", "reason", rootMessage(error)));
                } else if (response.online()) {
                    sender.sendMessage(messages.get(sender, "admin.find-online", "player", response.username(), "server", response.serverId()));
                } else {
                    sender.sendMessage(messages.get(sender, "admin.find-offline", "player", response.username()));
                }
            }));
    }

    @Subcommand("metrics")
    public void showMetrics(CommandSender sender) {
        MessageCatalog messages = core.messages();
        MetricsSnapshot snapshot = core.metrics().snapshot();
        sender.sendMessage(messages.get(sender, "admin.metrics",
            "published", Long.toString(snapshot.counterTotal("muxe_packets_published_total")),
            "received", Long.toString(snapshot.counterTotal("muxe_packets_received_total")),
            "duplicates", Long.toString(snapshot.counterTotal("muxe_packets_duplicates_total")),
            "rejected", Long.toString(snapshot.counterTotal("muxe_packets_rejected_total")),
            "failures", Long.toString(snapshot.counterTotal("muxe_packets_handler_failures_total")),
            "tps", String.format(Locale.ROOT, "%.2f", snapshot.gaugeTotal("muxe_server_tps"))));
    }

    @Subcommand("latencia")
    public void showLatency(CommandSender sender) {
        MessageCatalog messages = core.messages();
        MetricsSnapshot snapshot = core.metrics().snapshot();
        Map<String, Integer> pings = new HashMap<>();
        if (pingReader == null) {
            pingReader = PlayerPing.resolve();
        }
        pingReader.ifPresent(reader -> Bukkit.getOnlinePlayers().forEach(player -> reader.of(player).ifPresent(ping -> pings.put(player.getName(), ping))));
        if (!pings.isEmpty()) {
            sender.sendMessage(messages.get(sender, "admin.latency-pings", "players", PingList.describe(pings, messages.get("admin.latency-ping"))));
        }
        joins.ifPresent(traffic -> Bukkit.getOnlinePlayers().forEach(player -> traffic.report(player.getUniqueId()).ifPresent(report -> {
            sender.sendMessage(messages.get(sender, "admin.latency-join",
                "player", player.getName(),
                "ready", report.readyMillis().isPresent() ? Long.toString(report.readyMillis().getAsLong()) : "?",
                "total", Integer.toString(report.total()),
                "kb", Long.toString(report.kilobytes()),
                "wait", Long.toString(report.slowestWriteMillis()),
                "busiest", report.busiest(JOIN_PACKET_KINDS).stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(", "))));
            if (!report.probes().isEmpty()) {
                sender.sendMessage(messages.get(sender, "admin.latency-probes", "probes", report.probes().stream()
                    .map(probe -> messages.get(sender, "admin.latency-probe",
                        "at", String.format(Locale.ROOT, "%.1f", probe.sentAfterMillis() / 1000.0),
                        "answer", probe.answeredInMillis().isPresent() ? Long.toString(probe.answeredInMillis().getAsLong()) : "?"))
                    .collect(Collectors.joining(", "))));
            }
        })));
        sender.sendMessage(messages.get(sender, "admin.latency-pools",
            "db-active", whole(snapshot.gaugeTotal("muxe_db_connections_active")),
            "db-pending", whole(snapshot.gaugeTotal("muxe_db_connections_pending")),
            "active", whole(snapshot.gaugeTotal("muxe_executor_tasks_active")),
            "queued", whole(snapshot.gaugeTotal("muxe_executor_tasks_queued"))));
        List<SlowestTimers.Line> slowest = SlowestTimers.of(snapshot, LATENCY_LINES);
        if (slowest.isEmpty()) {
            sender.sendMessage(messages.get(sender, "admin.latency-empty"));
            return;
        }
        sender.sendMessage(messages.get(sender, "admin.latency-header"));
        for (SlowestTimers.Line line : slowest) {
            sender.sendMessage(messages.get(sender, "admin.latency-line",
                "label", line.label(),
                "count", Long.toString(line.count()),
                "mean", String.format(Locale.ROOT, "%.1f", line.meanMillis()),
                "max", String.format(Locale.ROOT, "%.1f", line.maxMillis())));
        }
    }

    private static String whole(double value) {
        return Long.toString(Math.round(value));
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
