package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HologramCommand implements CommandExecutor {

    public static final String NAME = "holograma";
    public static final String PERMISSION = "muxe.hologram";

    private final PlacementSync placements;
    private final MessageCatalog messages;
    private final AsyncReply reply;

    public HologramCommand(PlacementSync placements, MessageCatalog messages, AsyncReply reply) {
        this.placements = placements;
        this.messages = messages;
        this.reply = reply;
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
        switch (CommandText.word(args, 0)) {
            case "criar" -> create(player, args);
            case "adicionar" -> withHologram(player, args, 3, hologram -> reply.then(player, placements.save(hologram.adding(CommandText.line(args, 2))),
                saved -> player.sendMessage(messages.get("hologram-line-added", "id", hologram.id()))));
            case "linha" -> withLine(player, args, 4, (hologram, number) -> reply.then(player, placements.save(hologram.replacing(number, CommandText.line(args, 3))),
                saved -> player.sendMessage(messages.get("hologram-line-changed", "id", hologram.id(), "line", Integer.toString(number)))));
            case "removerlinha" -> withLine(player, args, 3, (hologram, number) -> {
                if (hologram.lines().size() == 1) {
                    player.sendMessage(messages.get("hologram-last-line"));
                    return;
                }
                reply.then(player, placements.save(hologram.without(number)),
                    saved -> player.sendMessage(messages.get("hologram-line-removed", "id", hologram.id(), "line", Integer.toString(number))));
            });
            case "ver" -> withHologram(player, args, 2, hologram -> show(player, hologram));
            case "mover" -> withHologram(player, args, 2, hologram -> reply.then(player, placements.save(hologram.movedTo(Positions.of(player.getLocation()))),
                saved -> player.sendMessage(messages.get("hologram-moved", "id", hologram.id()))));
            case "remover" -> withHologram(player, args, 2, hologram -> reply.then(player, placements.remove(PlacementKind.HOLOGRAM, hologram.id()),
                removed -> player.sendMessage(messages.get("hologram-removed", "id", hologram.id()))));
            case "listar" -> list(player);
            default -> usage(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 3) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        if (!PlacementIds.isValid(id)) {
            player.sendMessage(messages.get("hologram-invalid-id"));
            return;
        }
        if (placements.current().hologram(id).isPresent()) {
            player.sendMessage(messages.get("hologram-exists", "id", id));
            return;
        }
        HologramPlacement hologram = new HologramPlacement(placements.group(), id, Positions.of(player.getLocation()), List.of(CommandText.line(args, 2)));
        reply.then(player, placements.save(hologram), saved -> player.sendMessage(messages.get("hologram-created", "id", id)));
    }

    private void show(Player player, HologramPlacement hologram) {
        player.sendMessage(messages.get("hologram-lines-header", "id", hologram.id(), "location", hologram.position().describe()));
        for (int index = 0; index < hologram.lines().size(); index++) {
            String line = hologram.lines().get(index);
            player.sendMessage(messages.get("hologram-lines-line", "line", Integer.toString(index + 1), "text", line.isEmpty() ? CommandText.EMPTY_LINE : Colors.translate(line)));
        }
    }

    private void list(Player player) {
        List<HologramPlacement> all = placements.current().holograms();
        if (all.isEmpty()) {
            player.sendMessage(messages.get("hologram-list-empty"));
            return;
        }
        player.sendMessage(messages.get("hologram-list-header", "count", Integer.toString(all.size()), "group", placements.group()));
        for (HologramPlacement hologram : all) {
            player.sendMessage(messages.get("hologram-list-line",
                "id", hologram.id(),
                "lines", Integer.toString(hologram.lines().size()),
                "location", hologram.position().describe()));
        }
    }

    private void withLine(Player player, String[] args, int minimumArgs, LineAction action) {
        withHologram(player, args, minimumArgs, hologram -> {
            OptionalInt number = CommandText.number(args[2]);
            if (number.isEmpty() || !hologram.hasLine(number.getAsInt())) {
                player.sendMessage(messages.get("hologram-invalid-line", "count", Integer.toString(hologram.lines().size())));
                return;
            }
            action.run(hologram, number.getAsInt());
        });
    }

    private void withHologram(Player player, String[] args, int minimumArgs, Consumer<HologramPlacement> action) {
        if (args.length < minimumArgs) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        Optional<HologramPlacement> hologram = placements.current().hologram(id);
        if (hologram.isEmpty()) {
            player.sendMessage(messages.get("hologram-unknown", "id", id));
            return;
        }
        action.accept(hologram.get());
    }

    private void usage(Player player) {
        messages.lines("hologram-usage").forEach(player::sendMessage);
    }

    @FunctionalInterface
    private interface LineAction {
        void run(HologramPlacement hologram, int number);
    }
}
