package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Position;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Command(name = ParkourCommand.NAME, description = "Places the start, checkpoints and end of this server group's parkour.", usage = "parkour-usage")
@Permission(ParkourCommand.PERMISSION)
public final class ParkourCommand {

    public static final String NAME = "parkour";
    public static final String PERMISSION = "muxe.parkour";

    private static final String START_WORD = "inicio";
    private static final String END_WORD = "fim";
    private static final String CHECKPOINT_WORD = "checkpoint";
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
            case START_WORD, "início" -> Optional.of(ParkourPoint.start());
            case END_WORD -> Optional.of(ParkourPoint.end());
            case CHECKPOINT_WORD -> checkpoint(number);
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

    @Subcommand(value = START_WORD, aliases = "início")
    public void start(Player player) {
        place(player, Optional.of(ParkourPoint.start()));
    }

    @Subcommand(END_WORD)
    public void end(Player player) {
        place(player, Optional.of(ParkourPoint.end()));
    }

    @Subcommand(CHECKPOINT_WORD)
    public void checkpoint(Player player, @Arg("número") Optional<String> number) {
        place(player, point(CHECKPOINT_WORD, word(number)));
    }

    @Subcommand("remover")
    public void remove(Player player, @Arg("ponto") @Suggest({START_WORD, END_WORD, CHECKPOINT_WORD}) Optional<String> word, @Arg("número") Optional<String> number) {
        Optional<ParkourPoint> point = point(word(word), word(number));
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

    @Subcommand("listar")
    public void list(Player player) {
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

    private void place(Player player, Optional<ParkourPoint> point) {
        if (point.isEmpty()) {
            player.sendMessage(messages.get("parkour-invalid-point"));
            return;
        }
        ParkourPointPlacement placed = new ParkourPointPlacement(placements.group(), point.get(), standingOn(player.getLocation()));
        reply.then(player, placements.save(placed), saved -> player.sendMessage(messages.get("parkour-saved", "point", name(point.get()))));
    }

    private String name(ParkourPoint point) {
        return switch (point.type()) {
            case START -> messages.get("parkour-point-start");
            case END -> messages.get("parkour-point-end");
            case CHECKPOINT -> messages.get("parkour-point-checkpoint", "number", Integer.toString(point.number()));
        };
    }

    private static String word(Optional<String> typed) {
        return typed.map(value -> value.toLowerCase(Locale.ROOT)).orElse("");
    }

    private static Optional<ParkourPoint> checkpoint(String number) {
        OptionalInt parsed = CommandText.number(number);
        if (parsed.isEmpty() || parsed.getAsInt() < 1) {
            return Optional.empty();
        }
        return Optional.of(ParkourPoint.checkpoint(parsed.getAsInt()));
    }
}
