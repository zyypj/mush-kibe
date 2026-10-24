package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.buildmode.BuildRules;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BuildCommand implements CommandExecutor {

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
        if (!player.hasPermission(BuildMode.PERMISSION)) {
            player.sendMessage(messages.get("no-permission"));
            return true;
        }
        Optional<Boolean> wanted = BuildRules.wants(args.length == 0 ? "" : args[0], build.isBuilding(player));
        if (wanted.isEmpty()) {
            player.sendMessage(messages.get("build-usage"));
            return true;
        }
        if (wanted.get()) {
            player.sendMessage(messages.get(switch (build.enter(player)) {
                case CHANGED -> "build-enabled";
                case UNCHANGED -> "build-already-on";
                case NOT_ALLOWED -> "build-not-allowed";
            }));
        } else {
            player.sendMessage(messages.get(build.leave(player) == BuildMode.Change.CHANGED ? "build-disabled" : "build-already-off"));
        }
        return true;
    }
}
