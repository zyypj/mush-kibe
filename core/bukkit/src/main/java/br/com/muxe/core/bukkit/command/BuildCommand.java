package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.buildmode.BuildRules;
import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Optional;
import org.bukkit.entity.Player;

@Command(name = "build", description = "Turns build mode on or off on every server.", usage = "build-usage")
@Permission(BuildMode.PERMISSION)
public final class BuildCommand {

    private final BuildMode build;
    private final MessageCatalog messages;

    public BuildCommand(BuildMode build, MessageCatalog messages) {
        this.build = build;
        this.messages = messages;
    }

    @Default
    public void toggle(Player player, @Arg("on|off") @Suggest({"on", "off"}) Optional<String> choice) {
        Optional<Boolean> wanted = BuildRules.wants(choice.orElse(""), build.isBuilding(player));
        if (wanted.isEmpty()) {
            player.sendMessage(messages.get("build-usage"));
            return;
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
    }
}
