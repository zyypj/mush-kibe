package br.com.muxe.core.bukkit.customroom.menu;

import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class ModeSelector {

    private final Localized<CustomRoomSettings> localized;
    private final LanguageLookup languages;

    public ModeSelector(CustomRoomSettings settings) {
        this(Localized.same(settings), LanguageLookup.DEFAULT);
    }

    public ModeSelector(Localized<CustomRoomSettings> settings, LanguageLookup languages) {
        this.localized = settings;
        this.languages = languages;
    }

    public void open(Player player, Choice choice) {
        CustomRoomSettings settings = settingsOf(player);
        ConfigurationSection selector = settings.section("selector");
        String title = selector.getString("title", "Selecionar modo");
        Menu.Builder menu = Menu.builder(Colors.translate(title)).rows(selector.getInt("rows", 4));
        List<String> lore = selector.getStringList("lore");
        ConfigurationSection games = selector.getConfigurationSection("games");
        if (games != null) {
            for (String game : games.getKeys(false)) {
                ConfigurationSection entry = games.getConfigurationSection(game);
                String modes = entry.getString("menu", "");
                menu.button(entry.getInt("slot"), ItemBuilder.from(entry).lore(translate(lore)), context -> {
                    if (!modes.isBlank()) {
                        openModes(context.player(), modes, choice);
                    }
                });
            }
        }
        int back = selector.getInt("back-slot", -1);
        if (back >= 0 && choice.back() != null) {
            menu.button(back, backButton(settings, choice.backTitle()), context -> choice.back().accept(context.player()));
        }
        menu.build().open(player);
    }

    public void openModes(Player player, String menuId, Choice choice) {
        CustomRoomSettings settings = settingsOf(player);
        ConfigurationSection section = settings.section("mode-menus." + menuId);
        String game = section.getString("game", menuId);
        Menu.Builder menu = Menu.builder(Colors.translate(section.getString("title", "Selecionar modo"))).rows(section.getInt("rows", 4));
        List<String> lore = settings.section("selector").getStringList("lore");
        ConfigurationSection modes = section.getConfigurationSection("modes");
        if (modes != null) {
            for (String mode : modes.getKeys(false)) {
                ConfigurationSection entry = modes.getConfigurationSection(mode);
                menu.button(entry.getInt("slot"), ItemBuilder.from(entry).lore(translate(lore)), context -> {
                    if (choice.available().test(game, mode)) {
                        choice.chosen().accept(new Chosen(context.player(), game, mode, Colors.strip(entry.getString("name", mode)), modeIcon(entry)));
                    }
                });
            }
        }
        int back = section.getInt("back-slot", -1);
        if (back >= 0) {
            menu.button(back, backButton(settings, settings.section("selector").getString("title", "Selecionar modo")), context -> open(context.player(), choice));
        }
        menu.build().open(player);
    }

    public Optional<ItemBuilder> icon(String game, String mode) {
        return icon(Language.DEFAULT, game, mode);
    }

    public Optional<ItemBuilder> icon(Language language, String game, String mode) {
        ConfigurationSection menus = localized.in(language).section("mode-menus");
        for (String id : menus.getKeys(false)) {
            ConfigurationSection menu = menus.getConfigurationSection(id);
            if (menu.getString("game", id).equals(game) && menu.isConfigurationSection("modes." + mode)) {
                return Optional.of(modeIcon(menu.getConfigurationSection("modes." + mode)));
            }
        }
        return Optional.empty();
    }

    private CustomRoomSettings settingsOf(Player player) {
        return localized.in(languages.of(player.getUniqueId()));
    }

    private static ItemBuilder backButton(CustomRoomSettings settings, String title) {
        return OptionMenus.backButton(settings.texts(), title);
    }

    private static ItemBuilder modeIcon(ConfigurationSection entry) {
        return ItemBuilder.from(entry);
    }

    private static List<String> translate(List<String> lines) {
        return lines.stream().map(Colors::translate).toList();
    }

    public record Choice(BiPredicate<String, String> available, Consumer<Chosen> chosen, Consumer<Player> back, String backTitle) {
    }

    public record Chosen(Player player, String game, String mode, String name, ItemBuilder icon) {
    }
}
