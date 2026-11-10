package br.com.muxe.core.bukkit.language.menu;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;

public final class LanguageMenu {

    private final Localized<LanguageMenuSettings> settings;

    public LanguageMenu(Localized<LanguageMenuSettings> settings) {
        this.settings = settings;
    }

    public Menu build(Language current, BiConsumer<Player, Language> onPick) {
        LanguageMenuSettings own = settings.in(current);
        Menu.Builder menu = Menu.builder(own.title()).rows(own.rows());
        for (Map.Entry<Language, LanguageMenuSettings.Choice> entry : own.choices().entrySet()) {
            Language language = entry.getKey();
            LanguageMenuSettings.Choice choice = entry.getValue();
            menu.button(choice.slot(), choice.item().glow(language == current), context -> {
                context.close();
                onPick.accept(context.player(), language);
            });
        }
        return menu.build();
    }
}
