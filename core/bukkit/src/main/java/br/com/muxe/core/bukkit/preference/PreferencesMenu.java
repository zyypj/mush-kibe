package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.preference.domain.Preference;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class PreferencesMenu {

    public static final String SOON = "preferences-soon";
    public static final String FIRST_TAB = "geral";
    public static final String TAG_FORMAT = "tag-format";

    private final Localized<PreferencesSettings> settings;
    private final PreferenceService preferences;
    private final MessageCatalog messages;
    private final TagFormatMenu tagFormat;

    public PreferencesMenu(Localized<PreferencesSettings> settings, PreferenceService preferences, MessageCatalog messages, TagFormatMenu tagFormat) {
        this.settings = settings;
        this.preferences = preferences;
        this.messages = messages;
        this.tagFormat = tagFormat;
    }

    public void open(Player player, Optional<Consumer<Player>> back) {
        open(player, FIRST_TAB, back);
    }

    public void open(Player player, String tab, Optional<Consumer<Player>> back) {
        UUID id = player.getUniqueId();
        PreferencesSettings current = settings.in(messages.language(player));
        Menu.Builder menu = Menu.builder(current.title()).rows(current.rows());
        for (PreferencesPages.Shown shown : PreferencesPages.page(current, tab, preferences.of(id), back.isPresent())) {
            menu.button(shown.slot(), shown.icon(), context -> {
                switch (shown.click()) {
                    case NOTHING -> {
                    }
                    case TAB -> open(context.player(), shown.target().orElse(tab), back);
                    case CHANGE -> {
                        Preference preference = shown.preference().orElseThrow();
                        preferences.change(id, preference, preferences.of(id).next(preference));
                        open(context.player(), tab, back);
                    }
                    case OPEN -> {
                        if (shown.target().filter(TAG_FORMAT::equals).isPresent()) {
                            tagFormat.open(context.player(), Optional.of(viewer -> open(viewer, tab, back)));
                        } else {
                            context.player().sendMessage(messages.get(context.player(), SOON));
                        }
                    }
                    case SOON -> context.player().sendMessage(messages.get(context.player(), SOON));
                    case BACK -> back.ifPresent(open -> open.accept(context.player()));
                }
            });
        }
        menu.build().open(player);
    }
}
