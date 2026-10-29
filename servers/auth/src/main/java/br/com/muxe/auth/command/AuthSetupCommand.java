package br.com.muxe.auth.command;

import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.auth.setup.AuthLocations;
import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.hologram.PixelHolograms;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.location.LocationSlot;
import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Command(name = AuthSetupCommand.NAME, description = "Configures the spawn and the challenge code position.")
@Permission(AuthSetupCommand.PERMISSION)
public final class AuthSetupCommand {

    public static final String NAME = "authsetup";
    public static final String PERMISSION = "muxe.auth.setup";

    private static final String SPAWN = "spawn";
    private static final String CODE = "codigo";
    private static final String PREVIEW_CODE = "4829";
    private static final long PREVIEW_TICKS = 200L;

    private final Plugin plugin;
    private final AuthFlow flow;
    private final AuthLocations locations;
    private final PixelHolograms codeText;
    private final MessageCatalog messages;

    public AuthSetupCommand(Plugin plugin, AuthFlow flow, AuthLocations locations, PixelHolograms codeText, MessageCatalog messages) {
        this.plugin = plugin;
        this.flow = flow;
        this.locations = locations;
        this.codeText = codeText;
        this.messages = messages;
    }

    @Usage
    public void usage(Player player) {
        if (authenticated(player)) {
            player.sendMessage(messages.get("setup-usage"));
        }
    }

    @Subcommand(SPAWN)
    public void spawn(Player player) {
        if (authenticated(player)) {
            locations.spawn().set(player.getLocation());
            player.sendMessage(messages.get("setup-spawn-set", "location", describe(locations.spawn())));
        }
    }

    @Subcommand(CODE)
    public void code(Player player) {
        if (authenticated(player)) {
            locations.challengeCode().set(player.getEyeLocation());
            player.sendMessage(messages.get("setup-code-set", "location", describe(locations.challengeCode())));
        }
    }

    @Subcommand("testar")
    public void preview(Player player) {
        if (!authenticated(player)) {
            return;
        }
        Location viewerEyes = locations.spawnEyes(player.getEyeLocation());
        Location anchor = locations.challengeAnchor(viewerEyes, codeText.height());
        player.sendMessage(messages.get("setup-preview"));
        SpawnedHologram hologram = codeText.show(player, anchor, PREVIEW_CODE);
        plugin.getServer().getScheduler().runTaskLater(plugin, hologram::remove, PREVIEW_TICKS);
    }

    @Subcommand("info")
    public void info(Player player) {
        if (authenticated(player)) {
            messages.lines("setup-info", "spawn", describe(locations.spawn()), "code", describe(locations.challengeCode())).forEach(player::sendMessage);
        }
    }

    @Subcommand("limpar")
    public void clear(Player player, @Arg("alvo") @Suggest({SPAWN, CODE}) String typedTarget) {
        if (!authenticated(player)) {
            return;
        }
        String target = typedTarget.toLowerCase(Locale.ROOT);
        if (target.equals(SPAWN)) {
            locations.spawn().clear();
        } else if (target.equals(CODE)) {
            locations.challengeCode().clear();
        } else {
            player.sendMessage(messages.get("setup-usage"));
            return;
        }
        player.sendMessage(messages.get("setup-cleared", "target", target));
    }

    private boolean authenticated(Player player) {
        if (flow.isAuthenticated(player.getUniqueId())) {
            return true;
        }
        player.sendMessage(messages.get("setup-not-authenticated"));
        return false;
    }

    private String describe(LocationSlot slot) {
        return slot.stored().map(StoredLocation::describe).orElseGet(() -> messages.get("setup-default"));
    }
}
