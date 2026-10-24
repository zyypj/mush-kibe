package br.com.muxe.auth.command;

import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.auth.setup.AuthLocations;
import br.com.muxe.core.bukkit.hologram.PixelHolograms;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.location.LocationSlot;
import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class AuthSetupCommand implements CommandExecutor {

    public static final String NAME = "authsetup";
    public static final String PERMISSION = "muxe.auth.setup";

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
        if (!flow.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(messages.get("setup-not-authenticated"));
            return true;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("spawn")) {
            locations.spawn().set(player.getLocation());
            player.sendMessage(messages.get("setup-spawn-set", "location", describe(locations.spawn())));
            return true;
        }
        if (action.equals("codigo")) {
            locations.challengeCode().set(player.getEyeLocation());
            player.sendMessage(messages.get("setup-code-set", "location", describe(locations.challengeCode())));
            return true;
        }
        if (action.equals("testar")) {
            preview(player);
            return true;
        }
        if (action.equals("info")) {
            player.sendMessage(messages.get("setup-info-spawn", "location", describe(locations.spawn())));
            player.sendMessage(messages.get("setup-info-code", "location", describe(locations.challengeCode())));
            return true;
        }
        if (action.equals("limpar") && args.length == 2) {
            clear(player, args[1].toLowerCase(Locale.ROOT));
            return true;
        }
        player.sendMessage(messages.get("setup-usage"));
        return true;
    }

    private void clear(Player player, String target) {
        if (target.equals("spawn")) {
            locations.spawn().clear();
        } else if (target.equals("codigo")) {
            locations.challengeCode().clear();
        } else {
            player.sendMessage(messages.get("setup-usage"));
            return;
        }
        player.sendMessage(messages.get("setup-cleared", "target", target));
    }

    private void preview(Player player) {
        Location viewerEyes = locations.spawnEyes(player.getEyeLocation());
        Location anchor = locations.challengeAnchor(viewerEyes, codeText.height());
        player.sendMessage(messages.get("setup-preview"));
        SpawnedHologram hologram = codeText.show(player, anchor, PREVIEW_CODE);
        plugin.getServer().getScheduler().runTaskLater(plugin, hologram::remove, PREVIEW_TICKS);
    }

    private String describe(LocationSlot slot) {
        return slot.stored().map(StoredLocation::describe).orElseGet(() -> messages.get("setup-default"));
    }
}
