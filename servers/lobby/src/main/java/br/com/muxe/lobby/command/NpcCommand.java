package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.npc.GameNpc;
import br.com.muxe.lobby.npc.LobbyNpcs;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class NpcCommand implements CommandExecutor {

    public static final String NAME = "npc";
    public static final String PERMISSION = "muxe.lobby.npc";

    private final MuxeCore core;
    private final LobbyNpcs npcs;
    private final MessageCatalog messages;

    public NpcCommand(MuxeCore core, LobbyNpcs npcs, MessageCatalog messages) {
        this.core = core;
        this.npcs = npcs;
        this.messages = messages;
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
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "criar" -> create(player, args);
            case "mover" -> withNpc(player, args, 2, npc -> {
                npcs.save(npc.movedTo(LobbyNpcs.standingAt(player.getLocation())));
                player.sendMessage(messages.get("npc-moved", "id", npc.id()));
            });
            case "skin" -> withNpc(player, args, 3, npc -> changeSkin(player, npc, args[2]));
            case "nome" -> withNpc(player, args, 3, npc -> {
                String name = join(args, 2);
                npcs.save(npc.named(name));
                player.sendMessage(messages.get("npc-renamed", "id", npc.id(), "name", name));
            });
            case "remover" -> withNpc(player, args, 2, npc -> {
                npcs.remove(npc.id());
                player.sendMessage(messages.get("npc-removed", "id", npc.id()));
            });
            case "listar" -> list(player);
            default -> messages.lines("npc-usage").forEach(player::sendMessage);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 5) {
            messages.lines("npc-usage").forEach(player::sendMessage);
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!GameNpc.isValidId(id)) {
            player.sendMessage(messages.get("npc-invalid-id"));
            return;
        }
        if (npcs.find(id).isPresent()) {
            player.sendMessage(messages.get("npc-exists", "id", id));
            return;
        }
        String group = args[2].toLowerCase(Locale.ROOT);
        String skinName = args[3];
        String name = join(args, 4);
        GameNpc npc = new GameNpc(id, group, name, LobbyNpcs.standingAt(player.getLocation()), skinName, Optional.empty());
        npcs.save(npc);
        player.sendMessage(messages.get("npc-created", "id", id, "group", group));
        changeSkin(player, npc, skinName);
    }

    private void changeSkin(Player player, GameNpc npc, String skinName) {
        player.sendMessage(messages.get("npc-fetching-skin", "skin", skinName));
        npcs.skin(skinName).whenComplete((skin, error) -> core.mainThread().execute(() -> {
            Optional<GameNpc> current = npcs.find(npc.id());
            if (current.isEmpty()) {
                return;
            }
            if (error != null || skin == null || skin.isEmpty()) {
                npcs.save(current.get().wearing(skinName, Optional.empty()));
                if (player.isOnline()) {
                    player.sendMessage(messages.get("npc-skin-missing", "skin", skinName));
                }
                return;
            }
            npcs.save(current.get().wearing(skinName, skin));
            if (player.isOnline()) {
                player.sendMessage(messages.get("npc-skin-changed", "id", npc.id(), "skin", skinName));
            }
        }));
    }

    private void list(Player player) {
        Collection<GameNpc> all = npcs.all();
        if (all.isEmpty()) {
            player.sendMessage(messages.get("npc-list-empty"));
            return;
        }
        player.sendMessage(messages.get("npc-list-header", "count", Integer.toString(all.size())));
        for (GameNpc npc : all) {
            player.sendMessage(messages.get("npc-list-line",
                "id", npc.id(),
                "group", npc.group(),
                "name", npc.name(),
                "skin", npc.skinName().isEmpty() ? "-" : npc.skinName(),
                "location", npc.location().describe()));
        }
    }

    private void withNpc(Player player, String[] args, int minimumArgs, NpcAction action) {
        if (args.length < minimumArgs) {
            messages.lines("npc-usage").forEach(player::sendMessage);
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        Optional<GameNpc> npc = npcs.find(id);
        if (npc.isEmpty()) {
            player.sendMessage(messages.get("npc-unknown", "id", id));
            return;
        }
        action.run(npc.get());
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    @FunctionalInterface
    private interface NpcAction {
        void run(GameNpc npc);
    }
}
