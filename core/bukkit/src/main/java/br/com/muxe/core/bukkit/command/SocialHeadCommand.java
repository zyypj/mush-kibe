package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SocialHeadCommand implements CommandExecutor {

    public static final String NAME = "redes";
    public static final String PERMISSION = "muxe.social";

    private final PlacementSync placements;
    private final MessageCatalog messages;
    private final AsyncReply reply;

    public SocialHeadCommand(PlacementSync placements, MessageCatalog messages, AsyncReply reply) {
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
            case "mover" -> withHead(player, args, head -> reply.then(player, placements.save(head.movedTo(Positions.of(player.getLocation()))),
                saved -> player.sendMessage(messages.get("social-moved", "id", head.id()))));
            case "remover" -> withHead(player, args, head -> reply.then(player, placements.remove(PlacementKind.SOCIAL_HEAD, head.id()),
                removed -> player.sendMessage(messages.get("social-removed", "id", head.id()))));
            case "listar" -> list(player);
            default -> usage(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 2) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        if (!PlacementIds.isValid(id)) {
            player.sendMessage(messages.get("social-invalid-id"));
            return;
        }
        if (placements.current().socialHead(id).isPresent()) {
            player.sendMessage(messages.get("social-exists", "id", id));
            return;
        }
        SocialHeadPlacement head = new SocialHeadPlacement(placements.group(), id, Positions.of(player.getLocation()));
        reply.then(player, placements.save(head), saved -> player.sendMessage(messages.get("social-created", "id", id)));
    }

    private void list(Player player) {
        List<SocialHeadPlacement> all = placements.current().socialHeads();
        if (all.isEmpty()) {
            player.sendMessage(messages.get("social-list-empty"));
            return;
        }
        player.sendMessage(messages.get("social-list-header", "count", Integer.toString(all.size()), "group", placements.group()));
        for (SocialHeadPlacement head : all) {
            player.sendMessage(messages.get("social-list-line", "id", head.id(), "location", head.position().describe()));
        }
    }

    private void withHead(Player player, String[] args, Consumer<SocialHeadPlacement> action) {
        if (args.length < 2) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        Optional<SocialHeadPlacement> head = placements.current().socialHead(id);
        if (head.isEmpty()) {
            player.sendMessage(messages.get("social-unknown", "id", id));
            return;
        }
        action.accept(head.get());
    }

    private void usage(Player player) {
        messages.lines("social-usage").forEach(player::sendMessage);
    }
}
