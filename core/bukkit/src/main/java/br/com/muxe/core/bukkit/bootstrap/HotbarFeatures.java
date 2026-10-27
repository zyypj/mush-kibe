package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.hotbar.Hotbar;
import br.com.muxe.core.bukkit.hotbar.HotbarActions;
import br.com.muxe.core.bukkit.hotbar.HotbarBuildHook;
import br.com.muxe.core.bukkit.hotbar.HotbarListener;
import br.com.muxe.core.bukkit.hotbar.HotbarSettings;
import br.com.muxe.core.bukkit.menu.button.ButtonActions;
import br.com.muxe.core.bukkit.profile.ProfileMenu;
import br.com.muxe.core.bukkit.profile.ProfileSettings;
import br.com.muxe.core.bukkit.selector.game.GameSelectorMenu;
import br.com.muxe.core.bukkit.selector.game.GameSelectorSettings;
import br.com.muxe.core.bukkit.selector.lobby.LobbySelectorMenu;
import br.com.muxe.core.bukkit.selector.lobby.LobbySelectorSettings;
import br.com.muxe.core.bukkit.visibility.VisibilityService;
import br.com.muxe.core.common.visibility.domain.FriendDirectory;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class HotbarFeatures {

    private static final String HOTBAR_SECTION = "hotbar";

    private HotbarFeatures() {
    }

    static void start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, PlacedFeatures placed) {
        if (!config.isSet(HOTBAR_SECTION)) {
            plugin.getLogger().warning("The hotbar is off: config.yml has no '" + HOTBAR_SECTION + "' section");
            return;
        }
        HotbarSettings settings = HotbarSettings.from(section(config, HOTBAR_SECTION));
        if (!settings.appliesTo(core.identity().kind())) {
            return;
        }
        ButtonActions buttons = new ButtonActions(placed.games(), placed.shopAccess());
        GameSelectorMenu games = new GameSelectorMenu(GameSelectorSettings.from(section(config, "menus.game-selector")), buttons);
        ProfileMenu profile = new ProfileMenu(ProfileSettings.from(section(config, "menus.profile")), buttons, core.queries(), core.mainThread(), plugin.getLogger());
        LobbySelectorMenu lobbies = new LobbySelectorMenu(core, LobbySelectorSettings.from(section(config, "menus.lobby-selector"), core.identity().group()), core.messages());

        Hotbar hotbar = new Hotbar(settings);
        VisibilityService visibility = new VisibilityService(core, FriendDirectory.NONE, hotbar, core.messages(), plugin.getLogger());
        HotbarActions actions = new HotbarActions(games::open, profile::open, HotbarActions.NOTHING, visibility::toggle, lobbies::open);
        core.buildMode().addHook(new HotbarBuildHook(hotbar, visibility));
        plugin.getServer().getPluginManager().registerEvents(
            new HotbarListener(hotbar, actions, visibility, placed.spam(), core.buildMode(), core.messages()), plugin);
    }

    private static ConfigurationSection section(ConfigurationSection config, String path) {
        return Objects.requireNonNull(config.getConfigurationSection(path), "Missing configuration section " + path);
    }
}
