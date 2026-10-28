package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import java.util.List;
import org.bukkit.entity.Player;

@Command(name = HologramCommand.NAME, aliases = {"hologram", "holo"}, description = "Creates and manages the holograms of this server group.", usage = "hologram-usage")
@Permission(HologramCommand.PERMISSION)
public final class HologramCommand {

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

    @Subcommand("criar")
    public void create(Player player, @Arg("id") String typedId, @Arg("texto") @Greedy String text) {
        String id = CommandText.id(typedId);
        if (!PlacementIds.isValid(id)) {
            player.sendMessage(messages.get("hologram-invalid-id"));
            return;
        }
        if (placements.current().hologram(id).isPresent()) {
            player.sendMessage(messages.get("hologram-exists", "id", id));
            return;
        }
        HologramPlacement hologram = new HologramPlacement(placements.group(), id, Positions.of(player.getLocation()), List.of(CommandText.line(text)));
        reply.then(player, placements.save(hologram), saved -> player.sendMessage(messages.get("hologram-created", "id", id)));
    }

    @Subcommand("adicionar")
    public void addLine(Player player, @Arg("id") HologramPlacement hologram, @Arg("texto") @Greedy String text) {
        reply.then(player, placements.save(hologram.adding(CommandText.line(text))),
            saved -> player.sendMessage(messages.get("hologram-line-added", "id", hologram.id())));
    }

    @Subcommand("linha")
    public void changeLine(Player player, @Arg("id") HologramPlacement hologram, @Arg("número") int number, @Arg("texto") @Greedy String text) {
        if (validLine(player, hologram, number)) {
            reply.then(player, placements.save(hologram.replacing(number, CommandText.line(text))),
                saved -> player.sendMessage(messages.get("hologram-line-changed", "id", hologram.id(), "line", Integer.toString(number))));
        }
    }

    @Subcommand("removerlinha")
    public void removeLine(Player player, @Arg("id") HologramPlacement hologram, @Arg("número") int number) {
        if (!validLine(player, hologram, number)) {
            return;
        }
        if (hologram.lines().size() == 1) {
            player.sendMessage(messages.get("hologram-last-line"));
            return;
        }
        reply.then(player, placements.save(hologram.without(number)),
            saved -> player.sendMessage(messages.get("hologram-line-removed", "id", hologram.id(), "line", Integer.toString(number))));
    }

    @Subcommand("ver")
    public void show(Player player, @Arg("id") HologramPlacement hologram) {
        player.sendMessage(messages.get("hologram-lines-header", "id", hologram.id(), "location", hologram.position().describe()));
        for (int index = 0; index < hologram.lines().size(); index++) {
            String line = hologram.lines().get(index);
            player.sendMessage(messages.get("hologram-lines-line", "line", Integer.toString(index + 1), "text", line.isEmpty() ? CommandText.EMPTY_LINE : Colors.translate(line)));
        }
    }

    @Subcommand("mover")
    public void move(Player player, @Arg("id") HologramPlacement hologram) {
        reply.then(player, placements.save(hologram.movedTo(Positions.of(player.getLocation()))),
            saved -> player.sendMessage(messages.get("hologram-moved", "id", hologram.id())));
    }

    @Subcommand("remover")
    public void remove(Player player, @Arg("id") HologramPlacement hologram) {
        reply.then(player, placements.remove(PlacementKind.HOLOGRAM, hologram.id()),
            removed -> player.sendMessage(messages.get("hologram-removed", "id", hologram.id())));
    }

    @Subcommand("listar")
    public void list(Player player) {
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

    private boolean validLine(Player player, HologramPlacement hologram, int number) {
        if (number > 0 && hologram.hasLine(number)) {
            return true;
        }
        player.sendMessage(messages.get("hologram-invalid-line", "count", Integer.toString(hologram.lines().size())));
        return false;
    }
}
