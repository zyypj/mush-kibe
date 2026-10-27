package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Position;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ParkourCommand implements CommandExecutor {

    public static final String NAME = "parkour";
    public static final String PERMISSION = "muxe.parkour";

    private static final double CENTER = 0.5;

    private final PlacementSync placements;
    private final MessageCatalog messages;
    private final AsyncReply reply;

    public ParkourCommand(PlacementSync placements, MessageCatalog messages, AsyncReply reply) {
        this.placements = placements;
        this.messages = messages;
        this.reply = reply;
    }

    public static Optional<ParkourPoint> point(String word, String number) {
        return switch (word) {
            case "inicio", "início" -> Optional.of(ParkourPoint.start());
            case "fim" -> Optional.of(ParkourPoint.end());
            case "checkpoint" -> checkpoint(number);
            default -> checkpoint(word);
        };
    }

    public static Position standingOn(Location location) {
        return new Position(
            location.getWorld().getName(),
            location.getBlockX() + CENTER,
            location.getBlockY(),
            location.getBlockZ() + CENTER,
            location.getYaw(),
            0f
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(messages.get("no-permission"));
            return true;
        }
        String action = CommandText.word(args, 0);
        switch (action) {
            case "inicio", "início", "fim", "checkpoint" -> place(player, point(action, CommandText.word(args, 1)));
            case "remover" -> remove(player, point(CommandText.word(args, 1), CommandText.word(args, 2)));
            case "listar" -> list(player);
            default -> usage(player);
        }
        return true;
    }

    private void place(Player player, Optional<ParkourPoint> point) {
        if (point.isEmpty()) {
            player.sendMessage(messages.get("parkour-invalid-point"));
            return;
        }
        ParkourPointPlacement placed = new ParkourPointPlacement(placements.group(), point.get(), standingOn(player.getLocation()));
        reply.then(player, placements.save(placed), saved -> player.sendMessage(messages.get("parkour-saved", "point", name(point.get()))));
    }

    private void remove(Player player, Optional<ParkourPoint> point) {
        if (point.isEmpty()) {
            player.sendMessage(messages.get("parkour-invalid-point"));
            return;
        }
        if (placements.current().parkourPoint(point.get()).isEmpty()) {
            player.sendMessage(messages.get("parkour-unknown", "point", name(point.get())));
            return;
        }
        reply.then(player, placements.remove(PlacementKind.PARKOUR, point.get().id()),
            removed -> player.sendMessage(messages.get("parkour-removed", "point", name(point.get()))));
    }

    private void list(Player player) {
        List<ParkourPointPlacement> points = placements.current().parkourPoints().stream()
            .sorted(Comparator.comparing((ParkourPointPlacement placed) -> placed.point().type()).thenComparingInt(placed -> placed.point().number()))
            .toList();
        if (points.isEmpty()) {
            player.sendMessage(messages.get("parkour-list-empty"));
            return;
        }
        player.sendMessage(messages.get("parkour-list-header", "count", Integer.toString(points.size()), "group", placements.group()));
        for (ParkourPointPlacement placed : points) {
            player.sendMessage(messages.get("parkour-list-line", "point", name(placed.point()), "location", placed.position().describe()));
        }
    }

    private String name(ParkourPoint point) {
        return switch (point.type()) {
            case START -> messages.get("parkour-point-start");
            case END -> messages.get("parkour-point-end");
            case CHECKPOINT -> messages.get("parkour-point-checkpoint", "number", Integer.toString(point.number()));
        };
    }

    private void usage(Player player) {
        messages.lines("parkour-usage").forEach(player::sendMessage);
    }

    private static Optional<ParkourPoint> checkpoint(String number) {
        OptionalInt parsed = CommandText.number(number);
        if (parsed.isEmpty() || parsed.getAsInt() < 1) {
            return Optional.empty();
        }
        return Optional.of(ParkourPoint.checkpoint(parsed.getAsInt()));
    }
}
