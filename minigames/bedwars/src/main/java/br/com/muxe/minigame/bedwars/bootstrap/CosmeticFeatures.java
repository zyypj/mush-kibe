package br.com.muxe.minigame.bedwars.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer;
import br.com.muxe.core.bukkit.permission.command.Replies;
import br.com.muxe.core.bukkit.permission.command.TargetLookup;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.BedBreakCosmetics;
import br.com.muxe.minigame.bedwars.cosmetic.animation.BedAnimations;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.CosmeticCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.command.CosmeticCommand;
import br.com.muxe.minigame.bedwars.cosmetic.menu.BedWarsMenuItem;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenuSettings;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenus;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenus.PreviewGate;
import br.com.muxe.minigame.bedwars.cosmetic.menu.MenuSlot;
import br.com.muxe.minigame.bedwars.cosmetic.preview.BedPreview;
import br.com.muxe.minigame.bedwars.cosmetic.preview.PreviewSettings;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticProfileListener;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticProfiles;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class CosmeticFeatures {

    private final CosmeticMenus menus;
    private final BedBreakCosmetics bedBreaks;
    private final BedPreview preview;

    private CosmeticFeatures(CosmeticMenus menus, BedBreakCosmetics bedBreaks, BedPreview preview) {
        this.menus = menus;
        this.bedBreaks = bedBreaks;
        this.preview = preview;
    }

    static CosmeticFeatures start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, MessageCatalog messages, Commands commands, boolean lobby) {
        ConfigurationSection section = section(config, "cosmetics");
        BedAnimations animations = new BedAnimations();
        CosmeticCatalog catalog = CosmeticCatalog.from(section, id -> animations.find(id).isPresent());
        CosmeticProfiles profiles = new CosmeticProfiles(core.queries(), core.mainThread(), catalog.category(), id -> plugin.getServer().getPlayer(id) != null);
        CosmeticProfileListener profileListener = new CosmeticProfileListener(profiles, plugin.getLogger());
        plugin.getServer().getPluginManager().registerEvents(profileListener, plugin);
        plugin.getServer().getOnlinePlayers().forEach(profileListener::load);

        BedPreview preview = null;
        PreviewGate gate = (player, cosmetic) -> player.sendMessage(messages.get("preview-only-lobby"));
        if (lobby) {
            BedPreview started = new BedPreview(plugin, PreviewSettings.from(section(config, "preview")), animations, messages);
            if (started.start()) {
                preview = started;
                gate = started;
            }
        }
        CosmeticMenus menus = new CosmeticMenus(CosmeticMenuSettings.from(section(section, "menus")), catalog, profiles, core.commands(), core.mainThread(),
            messages, gate, plugin.getLogger());
        if (lobby) {
            BedWarsMenuItem item = new BedWarsMenuItem(plugin, MenuSlot.from(section(config, "lobby.menu-item")), menus);
            plugin.getServer().getPluginManager().registerEvents(item, plugin);
            core.buildMode().addHook(item);
            plugin.getServer().getOnlinePlayers().forEach(item::give);
        }
        TargetLookup targets = new TargetLookup(name -> Optional.ofNullable(plugin.getServer().getPlayerExact(name)), core.queries());
        commands.register(new CosmeticCommand(catalog, menus, core.commands(), targets, new Replies(core.mainThread(), messages, plugin.getLogger()), profileListener));
        return new CosmeticFeatures(menus, new BedBreakCosmetics(new AnimationPlayer(plugin), animations, catalog, profiles), preview);
    }

    BedBreakCosmetics bedBreaks() {
        return bedBreaks;
    }

    void openMenu(Player player) {
        menus.openBedWars(player);
    }

    boolean previewing(Player player) {
        return preview != null && preview.previewing(player);
    }

    void stop() {
        if (preview != null) {
            preview.stop();
        }
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + path);
    }
}
