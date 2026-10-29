package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import java.util.List;
import org.bukkit.entity.Player;

@Command(name = SocialHeadCommand.NAME, description = "Places the social media heads of this server group.", usage = "social-usage")
@Permission(SocialHeadCommand.PERMISSION)
public final class SocialHeadCommand {

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

    @Subcommand("criar")
    public void create(Player player, @Arg("id") String typedId) {
        String id = CommandText.id(typedId);
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

    @Subcommand("mover")
    public void move(Player player, @Arg("id") SocialHeadPlacement head) {
        reply.then(player, placements.save(head.movedTo(Positions.of(player.getLocation()))),
            saved -> player.sendMessage(messages.get("social-moved", "id", head.id())));
    }

    @Subcommand("remover")
    public void remove(Player player, @Arg("id") SocialHeadPlacement head) {
        reply.then(player, placements.remove(PlacementKind.SOCIAL_HEAD, head.id()),
            removed -> player.sendMessage(messages.get("social-removed", "id", head.id())));
    }

    @Subcommand("listar")
    public void list(Player player) {
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
}
