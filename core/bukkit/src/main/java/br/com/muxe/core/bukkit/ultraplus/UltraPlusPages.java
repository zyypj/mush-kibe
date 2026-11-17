package br.com.muxe.core.bukkit.ultraplus;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class UltraPlusPages {

    public static final int FULL = 100;

    private UltraPlusPages() {
    }

    public static List<Shown> main(UltraPlusSettings settings, Viewer viewer, boolean claimed, Instant now) {
        UltraPlusSettings.MainMenu menu = settings.main();
        UltraPlusSettings.Texts texts = settings.texts();
        UltraPlusLook look = viewer.look(settings);
        List<Shown> shown = new ArrayList<>();
        Duration left = Duration.between(now, nextMonth(now, settings.zone()));
        ItemBuilder reward = menu.reward().replacing("credits", Long.toString(settings.rewardCredits()))
            .addLore(" ", texts.resets().replace("{time}", settings.words().largest(left)), " ", claimed ? texts.claimed() : texts.claim());
        shown.add(new Shown(menu.rewardSlot(), reward, Click.REWARD, Optional.empty()));
        shown.add(new Shown(menu.colors().slot(), menu.colors().item(), Click.COLORS, Optional.empty()));
        List<UltraPlusSettings.Option> levels = menu.levels();
        for (int index = 0; index < levels.size(); index++) {
            UltraPlusSettings.Option level = levels.get(index);
            String example = settings.example(level, look.plus(), look.emblemText(), viewer.name());
            shown.add(option(texts, level, viewer, look.level().id().equals(level.id()), example, Click.LEVEL));
            if (index + 1 < levels.size()) {
                UltraPlusSettings.Option next = levels.get(index + 1);
                shown.add(new Shown((level.slot() + next.slot()) / 2, pane(menu, texts, viewer.months(), next.months()), Click.NOTHING, Optional.empty()));
            }
        }
        shown.add(new Shown(menu.back().slot(), menu.back().item(), Click.BACK, Optional.empty()));
        return shown;
    }

    public static List<Shown> colors(UltraPlusSettings settings, Viewer viewer) {
        UltraPlusSettings.OptionMenu menu = settings.colors();
        UltraPlusLook look = viewer.look(settings);
        List<Shown> shown = new ArrayList<>();
        for (UltraPlusSettings.Option color : menu.options()) {
            String example = settings.example(look.level(), color, look.emblemText(), viewer.name());
            shown.add(option(settings.texts(), color, viewer, look.plus().id().equals(color.id()), example, Click.PLUS));
        }
        shown.add(new Shown(menu.back().slot(), menu.back().item(), Click.BACK, Optional.empty()));
        return shown;
    }

    public static List<Shown> emblems(UltraPlusSettings settings, Viewer viewer) {
        UltraPlusSettings.EmblemMenu menu = settings.emblems();
        UltraPlusSettings.Texts texts = settings.texts();
        UltraPlusLook look = viewer.look(settings);
        List<Shown> shown = new ArrayList<>();
        boolean noneChosen = look.emblem().isEmpty();
        shown.add(new Shown(menu.noneSlot(), menu.none().lore(" ", viewer.member() && noneChosen ? texts.emblemSelected() : texts.select())
            .glow(viewer.member() && noneChosen), Click.EMBLEM, Optional.empty()));
        for (UltraPlusSettings.Emblem emblem : menu.emblems()) {
            boolean chosen = look.emblem().filter(current -> current.id().equals(emblem.id())).isPresent();
            String example = settings.example(look.level(), look.plus(), emblem.render(), viewer.name());
            String state = !viewer.member() ? texts.locked() : chosen ? texts.emblemSelected() : texts.select();
            ItemBuilder icon = emblem.item().lore(" ", texts.exampleTitle(), example, " ", texts.exclusive(), " ", state).glow(viewer.member() && chosen);
            shown.add(new Shown(emblem.slot(), icon, Click.EMBLEM, Optional.of(emblem.id())));
        }
        shown.add(new Shown(menu.back().slot(), menu.back().item(), Click.BACK, Optional.empty()));
        return shown;
    }

    public static YearMonth month(Instant now, ZoneId zone) {
        return YearMonth.from(now.atZone(zone));
    }

    public static Instant nextMonth(Instant now, ZoneId zone) {
        return month(now, zone).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
    }

    public static int percent(int months, int required) {
        if (required <= 0) {
            return FULL;
        }
        return (int) Math.min(FULL, (long) Math.max(0, months) * FULL / required);
    }

    static String progress(UltraPlusSettings.Texts texts, int months, int required) {
        return texts.progress().replace("{months}", Integer.toString(Math.min(Math.max(0, months), required))).replace("{required}", Integer.toString(required))
            .replace("{percent}", Integer.toString(percent(months, required)));
    }

    static String bar(int dashes, int percent) {
        int filled = percent * dashes / FULL;
        return (filled > 0 ? "&a" + "-".repeat(filled) : "") + "&7" + "-".repeat(dashes - filled) + ">";
    }

    private static Shown option(UltraPlusSettings.Texts texts, UltraPlusSettings.Option option, Viewer viewer, boolean current, String example, Click click) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        if (option.months() <= 0) {
            lore.add(texts.included());
        } else {
            lore.add(texts.required());
            lore.add(progress(texts, viewer.months(), option.months()));
        }
        lore.add(" ");
        lore.add(texts.exampleTitle());
        lore.add(example);
        lore.add(" ");
        boolean unlocked = viewer.member() && viewer.months() >= option.months();
        boolean selected = unlocked && current;
        lore.add(!unlocked ? texts.locked() : selected ? texts.selected() : texts.select());
        return new Shown(option.slot(), option.item().lore(lore).glow(selected), click, Optional.of(option.id()));
    }

    private static ItemBuilder pane(UltraPlusSettings.MainMenu menu, UltraPlusSettings.Texts texts, int months, int required) {
        return menu.pane().name(bar(menu.dashes(), percent(months, required))).lore(" ", texts.required(), progress(texts, months, required));
    }

    public enum Click {
        NOTHING,
        REWARD,
        COLORS,
        LEVEL,
        PLUS,
        EMBLEM,
        BACK
    }

    public record Shown(int slot, ItemBuilder icon, Click click, Optional<String> value) {

        public Shown {
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(click, "click");
            Objects.requireNonNull(value, "value");
        }
    }

    public record Viewer(String name, boolean member, int months, UltraPlusStyle style) {

        public Viewer {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(style, "style");
        }

        public UltraPlusLook look(UltraPlusSettings settings) {
            return UltraPlusLook.of(settings, style, months);
        }
    }
}
