package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.config.DefaultedSections;
import br.com.muxe.core.bukkit.customroom.CatalogDirectory;
import br.com.muxe.core.bukkit.customroom.CustomRoomKit;
import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.customroom.lobby.CustomRoomCommand;
import br.com.muxe.core.bukkit.customroom.lobby.CustomRoomLobby;
import br.com.muxe.core.bukkit.customroom.menu.MapSelector;
import br.com.muxe.core.bukkit.customroom.menu.ModeSelector;
import br.com.muxe.core.bukkit.customroom.menu.OptionMenus;
import br.com.muxe.core.bukkit.customroom.menu.PresetMenu;
import br.com.muxe.core.bukkit.prompt.AnvilEdits;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.function.Consumer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

final class CustomRoomFeatures {

    static final String SHOP_CATEGORY = "ultra-plus";

    private CustomRoomFeatures() {
    }

    static CustomRoomKit kit(JavaPlugin plugin, MuxeCore core, FileConfiguration config) {
        Localized<CustomRoomSettings> localized = core.texts().map(root -> CustomRoomSettings.from(DefaultedSections.of(root, "custom-room")));
        CustomRoomSettings settings = localized.standard();
        AnvilEdits anvils = AnvilEdits.start(plugin);
        OptionMenus options = new OptionMenus(anvils, player -> player.sendMessage(core.messages().get(player, "custom-room-invalid-value")));
        return new CustomRoomKit(settings, new CatalogDirectory(core.clock(), plugin.getLogger()), anvils, new ModeSelector(localized, core.languages()),
            new MapSelector(localized, core.languages()), options, new PresetMenu(core, localized, anvils), localized);
    }

    static void lobby(JavaPlugin plugin, MuxeCore core, Commands commands, Consumer<Player> gameSelector, Consumer<Player> shop) {
        CustomRoomKit kit = core.customRooms();
        CustomRoomLobby lobby = new CustomRoomLobby(core, kit.localized(), kit.catalogs(), kit.modes(), kit.maps(), kit.options(), kit.presets(), gameSelector, shop);
        lobby.start();
        commands.register(new CustomRoomCommand(lobby));
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                lobby.forget(event.getPlayer().getUniqueId());
            }
        }, plugin);
    }
}
