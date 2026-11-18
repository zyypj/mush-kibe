package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.preference.domain.Preference;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.entity.Player;

public final class TagFormatMenu {

    public static final String ONLY = "ultra-plus-only";

    private final Localized<TagFormatSettings> settings;
    private final PreferenceService preferences;
    private final Function<UUID, RankDisplay> displays;
    private final Predicate<UUID> members;
    private final MessageCatalog messages;

    public TagFormatMenu(Localized<TagFormatSettings> settings, PreferenceService preferences, Function<UUID, RankDisplay> displays, Predicate<UUID> members,
                         MessageCatalog messages) {
        this.settings = settings;
        this.preferences = preferences;
        this.displays = displays;
        this.members = members;
        this.messages = messages;
    }

    public void open(Player player, Optional<Consumer<Player>> back) {
        UUID id = player.getUniqueId();
        TagFormatSettings current = settings.in(messages.language(player));
        TagFormatPages.Viewer viewer = new TagFormatPages.Viewer(player.getName(), displays.apply(id), members.test(id), preferences.of(id).tagFormat());
        Menu.Builder menu = Menu.builder(current.title()).rows(current.rows());
        for (TagFormatPages.Shown shown : TagFormatPages.page(current, viewer, back.isPresent())) {
            menu.button(shown.slot(), shown.icon(), context -> {
                switch (shown.click()) {
                    case SELECT -> {
                        preferences.change(id, Preference.TAG_FORMAT, shown.format().orElseThrow().name());
                        open(context.player(), back);
                    }
                    case LOCKED -> context.player().sendMessage(messages.get(context.player(), ONLY));
                    case BACK -> back.ifPresent(open -> open.accept(context.player()));
                }
            });
        }
        menu.build().open(player);
    }
}
