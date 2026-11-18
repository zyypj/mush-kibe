package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.permission.display.TagFormatLook;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.preference.domain.TagFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TagFormatPages {

    private TagFormatPages() {
    }

    public static List<Shown> page(TagFormatSettings settings, Viewer viewer, boolean withBack) {
        TagFormatSettings.Texts texts = settings.texts();
        TagFormat current = viewer.member() ? viewer.format() : TagFormat.DEFAULT;
        List<Shown> shown = new ArrayList<>();
        for (TagFormatSettings.Option option : settings.options()) {
            boolean locked = option.exclusive() && !viewer.member();
            boolean selected = option.format() == current;
            String example = TagFormatLook.apply(option.format(), viewer.display()).preview(settings.example(), viewer.name(), "");
            List<String> lore = new ArrayList<>(List.of(" ", texts.example(), example, " "));
            if (option.exclusive()) {
                lore.add(texts.exclusive());
                lore.add(" ");
            }
            lore.add(selected ? texts.selected() : locked ? texts.locked() : texts.select());
            ItemBuilder icon = option.item().lore(lore.stream().map(Colors::translate).toList()).glow(selected);
            shown.add(new Shown(option.slot(), icon, locked ? Click.LOCKED : Click.SELECT, Optional.of(option.format())));
        }
        if (withBack) {
            shown.add(new Shown(settings.back().slot(), settings.back().item(), Click.BACK, Optional.empty()));
        }
        return shown;
    }

    public record Viewer(String name, RankDisplay display, boolean member, TagFormat format) {
    }

    public enum Click {
        SELECT,
        LOCKED,
        BACK
    }

    public record Shown(int slot, ItemBuilder icon, Click click, Optional<TagFormat> format) {
    }
}
