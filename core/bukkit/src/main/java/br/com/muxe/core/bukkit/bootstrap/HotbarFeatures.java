package br.com.muxe.core.bukkit.bootstrap;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.hotbar.Hotbar;
import br.com.muxe.core.bukkit.hotbar.HotbarActions;
import br.com.muxe.core.bukkit.hotbar.HotbarBuildHook;
import br.com.muxe.core.bukkit.hotbar.HotbarListener;
import br.com.muxe.core.bukkit.hotbar.HotbarSettings;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.language.PlayerLanguages;
import br.com.muxe.core.bukkit.menu.button.ButtonActions;
import br.com.muxe.core.bukkit.profile.ProfileMenu;
import br.com.muxe.core.bukkit.profile.ProfileSettings;
import br.com.muxe.core.bukkit.selector.game.GameSelectorMenu;
import br.com.muxe.core.bukkit.selector.game.GameSelectorSettings;
import br.com.muxe.core.bukkit.selector.lobby.LobbySelectorMenu;
import br.com.muxe.core.bukkit.selector.lobby.LobbySelectorSettings;
import br.com.muxe.core.bukkit.visibility.VisibilityService;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.visibility.domain.FriendDirectory;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class HotbarFeatures {

    private static final String HOTBAR_SECTION = "hotbar";

    private HotbarFeatures() {
    }

    static void start(JavaPlugin plugin, MuxeCore core, FileConfiguration config, PlacedFeatures placed, Commands commands) {
        if (!config.isSet(HOTBAR_SECTION)) {
            plugin.getLogger().warning("The hotbar is off: config.yml has no '" + HOTBAR_SECTION + "' section");
            return;
        }
        LocalizedConfig texts = core.texts();
        Localized<HotbarSettings> settings = texts.map(root -> HotbarSettings.from(section(root, HOTBAR_SECTION)));
        if (!settings.standard().appliesTo(core.identity().kind())) {
            return;
        }
        PlayerLanguages languages = core.languages();
        ButtonActions buttons = new ButtonActions(placed.games(), placed.shopAccess());
        GameSelectorMenu games = new GameSelectorMenu(texts.map(root -> GameSelectorSettings.from(section(root, "menus.game-selector"))), buttons, languages);
        CustomRoomFeatures.lobby(plugin, core, commands, games::open, player -> placed.shopAccess().open(player, CustomRoomFeatures.SHOP_CATEGORY));
        ProfileMenu profile = new ProfileMenu(texts.map(root -> ProfileSettings.from(section(root, "menus.profile"))), languages, buttons, core.queries(), core.permissions(),
            core.mainThread(), plugin.getLogger());
        LobbySelectorMenu lobbies = new LobbySelectorMenu(core, texts.map(root -> LobbySelectorSettings.from(section(root, "menus.lobby-selector"), core.identity().group())),
            core.messages());

        Hotbar hotbar = new Hotbar(settings, languages);
        VisibilityService visibility = new VisibilityService(core, FriendDirectory.NONE, hotbar, core.messages(), plugin.getLogger());
        HotbarActions actions = new HotbarActions(games::open, profile::open, HotbarActions.NOTHING, visibility::toggle, lobbies::open);
        core.buildMode().addHook(new HotbarBuildHook(hotbar, visibility));
        plugin.getServer().getPluginManager().registerEvents(
            new HotbarListener(hotbar, actions, visibility, placed.spam(), core.buildMode(), core.messages()), plugin);
        languages.onChange(player -> {
            if (!core.buildMode().isBuilding(player)) {
                hotbar.refresh(player, visibility.of(player.getUniqueId()));
            }
        });
    }

    private static ConfigurationSection section(ConfigurationSection config, String path) {
        return Objects.requireNonNull(config.getConfigurationSection(path), "Missing configuration section " + path);
    }
}
