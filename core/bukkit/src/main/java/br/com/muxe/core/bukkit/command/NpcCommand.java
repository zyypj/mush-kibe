package br.com.muxe.core.bukkit.command;

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
import java.util.function.Consumer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class NpcCommand implements CommandExecutor {

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
            case "acao" -> withNpc(player, args, 3, npc -> {
                NpcAction action = NpcAction.parse(args[2]);
                reply.then(player, placements.save(npc.doing(action)),
                    saved -> player.sendMessage(messages.get("npc-action-changed", "id", npc.id(), "action", action.describe())));
            });
            case "mover" -> withNpc(player, args, 2, npc -> reply.then(player, placements.save(npc.movedTo(Positions.standing(player.getLocation()))),
                saved -> player.sendMessage(messages.get("npc-moved", "id", npc.id()))));
            case "skin" -> withNpc(player, args, 3, npc -> changeSkin(player, npc, args[2]));
            case "nome" -> withNpc(player, args, 3, npc -> {
                String name = CommandText.join(args, 2);
                reply.then(player, placements.save(npc.named(name)),
                    saved -> player.sendMessage(messages.get("npc-renamed", "id", npc.id(), "name", name)));
            });
            case "remover" -> withNpc(player, args, 2, npc -> reply.then(player, placements.remove(PlacementKind.NPC, npc.id()),
                removed -> player.sendMessage(messages.get("npc-removed", "id", npc.id()))));
            case "listar" -> list(player);
            default -> usage(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 5) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        if (!PlacementIds.isValid(id)) {
            player.sendMessage(messages.get("npc-invalid-id"));
            return;
        }
        if (placements.current().npc(id).isPresent()) {
            player.sendMessage(messages.get("npc-exists", "id", id));
            return;
        }
        NpcAction action = NpcAction.parse(args[2]);
        String skinName = args[3];
        NpcPlacement npc = new NpcPlacement(placements.group(), id, Positions.standing(player.getLocation()), CommandText.join(args, 4), action, skinName, Optional.empty());
        reply.then(player, placements.save(npc), saved -> {
            player.sendMessage(messages.get("npc-created", "id", id, "action", action.describe()));
            changeSkin(player, npc, skinName);
        });
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

    private void list(Player player) {
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

    private void withNpc(Player player, String[] args, int minimumArgs, Consumer<NpcPlacement> action) {
        if (args.length < minimumArgs) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        Optional<NpcPlacement> npc = placements.current().npc(id);
        if (npc.isEmpty()) {
            player.sendMessage(messages.get("npc-unknown", "id", id));
            return;
        }
        action.accept(npc.get());
    }

    private void usage(Player player) {
        messages.lines("npc-usage").forEach(player::sendMessage);
    }
}
