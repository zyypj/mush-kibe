package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.MenuLayout;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.preference.domain.Preferences;
import br.com.muxe.core.common.preference.domain.PrivateMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;

public final class PreferencesPages {

    public static final int ROW_BELOW = 9;
    public static final short PANE_GRAY = 7;
    public static final short PANE_LIME = 5;
    public static final short DYE_ON = 10;
    public static final short DYE_OFF = 8;

    private PreferencesPages() {
    }

    public static List<Shown> page(PreferencesSettings settings, String tabId, Preferences preferences, boolean withBack) {
        PreferencesSettings.Tab open = settings.tab(tabId);
        List<Shown> shown = new ArrayList<>();
        for (PreferencesSettings.Tab tab : settings.tabs()) {
            List<String> lore = (tab.id().equals(open.id()) ? settings.tabSelected() : settings.tabUnselected()).stream().map(Colors::translate).toList();
            shown.add(new Shown(tab.button().slot(), tab.button().item().lore(lore), Click.TAB, Optional.empty(), Optional.of(tab.id())));
        }
        int openColumn = open.button().slot() % MenuLayout.ROW_WIDTH;
        for (int column = 0; column < MenuLayout.ROW_WIDTH; column++) {
            ItemBuilder pane = ItemBuilder.of(Material.STAINED_GLASS_PANE, column == openColumn ? PANE_LIME : PANE_GRAY).name(" ");
            shown.add(new Shown(settings.firstPane() + column, pane, Click.NOTHING, Optional.empty(), Optional.empty()));
        }
        for (PreferencesSettings.Entry entry : open.entries()) {
            if (entry.preference().isEmpty()) {
                ItemBuilder icon = entry.item();
                shown.add(new Shown(entry.slot(), icon, Click.SOON, Optional.empty(), Optional.empty()));
                entry.below().ifPresent(material -> shown.add(new Shown(entry.slot() + ROW_BELOW, ItemBuilder.of(material).name(icon.name()).lore(icon.lore()),
                    Click.SOON, Optional.empty(), Optional.empty())));
                continue;
            }
            Preference preference = entry.preference().get();
            Rendered rendered = render(settings.texts(), entry.item(), preference, preferences);
            shown.add(new Shown(entry.slot(), entry.item().name(rendered.name()).lore(rendered.lore()), Click.CHANGE, Optional.of(preference), Optional.empty()));
            ItemBuilder dye = ItemBuilder.of(Material.INK_SACK, rendered.dye()).name(rendered.name()).lore(rendered.lore());
            shown.add(new Shown(entry.slot() + ROW_BELOW, dye, Click.CHANGE, Optional.of(preference), Optional.empty()));
        }
        if (withBack) {
            shown.add(new Shown(settings.back().slot(), settings.back().item(), Click.BACK, Optional.empty(), Optional.empty()));
        }
        return shown;
    }

    static Rendered render(PreferencesSettings.Texts texts, ItemBuilder item, Preference preference, Preferences preferences) {
        String base = Colors.strip(item.name() == null ? "" : item.name());
        List<String> lore = new ArrayList<>(item.lore());
        lore.add(" ");
        if (preference.kind() == Preference.Kind.TOGGLE) {
            boolean on = preferences.on(preference);
            lore.add(Colors.translate(texts.value().replace("{value}", on ? texts.enabled() : texts.disabled())));
            lore.add(" ");
            lore.add(Colors.translate(on ? texts.clickDisable() : texts.clickEnable()));
            return new Rendered(name(texts, base, on), lore, on ? DYE_ON : DYE_OFF);
        }
        PrivateMessages current = preferences.privateMessages();
        for (PrivateMessages option : PrivateMessages.values()) {
            String line = option == current ? texts.optionSelected() : texts.optionOther();
            lore.add(Colors.translate(line.replace("{option}", texts.options().get(option))));
        }
        lore.add(" ");
        lore.add(Colors.translate(texts.clickCycle().replace("{option}", texts.options().get(current.next()))));
        return new Rendered(name(texts, base, current != PrivateMessages.OFF), lore, texts.optionDyes().get(current).shortValue());
    }

    private static String name(PreferencesSettings.Texts texts, String base, boolean on) {
        return Colors.translate((on ? texts.nameOn() : texts.nameOff()).replace("{name}", base));
    }

    record Rendered(String name, List<String> lore, short dye) {
    }

    public enum Click {
        NOTHING,
        TAB,
        CHANGE,
        SOON,
        BACK
    }

    public record Shown(int slot, ItemBuilder icon, Click click, Optional<Preference> preference, Optional<String> tab) {
    }
}
