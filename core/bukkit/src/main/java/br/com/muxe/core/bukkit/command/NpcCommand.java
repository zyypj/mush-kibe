package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.npc.placed.PlacedNpcs;
import br.com.muxe.core.bukkit.npc.skin.MojangSkins;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;

@Command(name = NpcCommand.NAME, description = "Creates and manages the NPCs of this server group.", usage = "npc-usage")
@Permission(NpcCommand.PERMISSION)
public final class NpcCommand {

    public static final String NAME = "npc";
    public static final String PERMISSION = "muxe.npc";

    private final PlacementSync placements;
    private final MojangSkins skins;
    private final Executor mainThread;
    private final MessageCatalog messages;
    private final AsyncReply reply;

    public NpcCommand(PlacementSync placements, MojangSkins skins, Executor mainThread, MessageCatalog messages, AsyncReply reply) {
        this.placements = placements;
        this.skins = skins;
        this.mainThread = mainThread;
        this.messages = messages;
        this.reply = reply;
    }

    @Subcommand("criar")
    public void create(
        Player player,
        @Arg("id") String typedId,
        @Arg("ação") @Suggest({NpcAction.SHOP_WORD, NpcAction.NONE_WORD}) String actionWord,
        @Arg("skin") String skinName,
        @Arg("nome") @Greedy String name
    ) {
        String id = CommandText.id(typedId);
        if (!PlacementIds.isValid(id)) {
            player.sendMessage(messages.get("npc-invalid-id"));
            return;
        }
        if (placements.current().npc(id).isPresent()) {
            player.sendMessage(messages.get("npc-exists", "id", id));
            return;
        }
        NpcAction action = NpcAction.parse(actionWord);
        NpcPlacement npc = new NpcPlacement(placements.group(), id, Positions.standing(player.getLocation()), name, action, skinName, Optional.empty());
        reply.then(player, placements.save(npc), saved -> {
            player.sendMessage(messages.get("npc-created", "id", id, "action", action.describe()));
            changeSkin(player, npc, skinName);
        });
    }

    @Subcommand("acao")
    public void changeAction(Player player, @Arg("id") NpcPlacement npc, @Arg("ação") @Suggest({NpcAction.SHOP_WORD, NpcAction.NONE_WORD}) String actionWord) {
        NpcAction action = NpcAction.parse(actionWord);
        reply.then(player, placements.save(npc.doing(action)),
            saved -> player.sendMessage(messages.get("npc-action-changed", "id", npc.id(), "action", action.describe())));
    }

    @Subcommand("mover")
    public void move(Player player, @Arg("id") NpcPlacement npc) {
        reply.then(player, placements.save(npc.movedTo(Positions.standing(player.getLocation()))),
            saved -> player.sendMessage(messages.get("npc-moved", "id", npc.id())));
    }

    @Subcommand("skin")
    public void skin(Player player, @Arg("id") NpcPlacement npc, @Arg("skin") String skinName) {
        changeSkin(player, npc, skinName);
    }

    @Subcommand("nome")
    public void rename(Player player, @Arg("id") NpcPlacement npc, @Arg("nome") @Greedy String name) {
        reply.then(player, placements.save(npc.named(name)),
            saved -> player.sendMessage(messages.get("npc-renamed", "id", npc.id(), "name", name)));
    }

    @Subcommand("remover")
    public void remove(Player player, @Arg("id") NpcPlacement npc) {
        reply.then(player, placements.remove(PlacementKind.NPC, npc.id()),
            removed -> player.sendMessage(messages.get("npc-removed", "id", npc.id())));
    }

    @Subcommand("listar")
    public void list(Player player) {
        List<NpcPlacement> all = placements.current().npcs();
        if (all.isEmpty()) {
            player.sendMessage(messages.get("npc-list-empty"));
            return;
        }
        player.sendMessage(messages.get("npc-list-header", "count", Integer.toString(all.size()), "group", placements.group()));
        for (NpcPlacement npc : all) {
            player.sendMessage(messages.get("npc-list-line",
                "id", npc.id(),
                "action", npc.action().describe(),
                "name", npc.name(),
                "skin", npc.skinName().isEmpty() ? "-" : npc.skinName(),
                "location", npc.position().describe()));
        }
    }

    private void changeSkin(Player player, NpcPlacement npc, String skinName) {
        player.sendMessage(messages.get("npc-fetching-skin", "skin", skinName));
        skins.byName(skinName).whenCompleteAsync((skin, error) -> {
            Optional<NpcPlacement> current = placements.current().npc(npc.id());
            if (current.isEmpty()) {
                return;
            }
            Optional<SkinTexture> texture = error == null && skin != null ? skin.map(PlacedNpcs::texture) : Optional.empty();
            reply.then(player, placements.save(current.get().wearing(skinName, texture)), saved -> player.sendMessage(texture.isPresent()
                ? messages.get("npc-skin-changed", "id", npc.id(), "skin", skinName)
                : messages.get("npc-skin-missing", "skin", skinName)));
        }, mainThread);
    }
}
