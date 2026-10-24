package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.build.BuildMode;
import br.com.muxe.lobby.build.BuildRules;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BuildCommand implements CommandExecutor {

    public static final String NAME = "build";
    public static final String PERMISSION = "muxe.lobby.build";

    private final BuildMode build;
    private final MessageCatalog messages;

    public BuildCommand(BuildMode build, MessageCatalog messages) {
        this.build = build;
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
        Optional<Boolean> wanted = BuildRules.wants(args.length == 0 ? "" : args[0], build.isBuilding(player));
        if (wanted.isEmpty()) {
            player.sendMessage(messages.get("build-usage"));
            return true;
        }
        if (wanted.get()) {
            player.sendMessage(messages.get(build.enter(player) ? "build-enabled" : "build-already-on"));
        } else {
            player.sendMessage(messages.get(build.leave(player) ? "build-disabled" : "build-already-off"));
        }
        return true;
    }
}
