package br.com.muxe.core.bukkit.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;

public record MenuButton(ItemBuilder icon, MenuClick click) {

    public MenuButton {
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(click, "click");
    }

    public static MenuButton of(ItemBuilder icon) {
        return new MenuButton(icon, MenuClick.NONE);
    }

    public static MenuButton of(ItemBuilder icon, MenuClick click) {
        return new MenuButton(icon, click);
    }
}
