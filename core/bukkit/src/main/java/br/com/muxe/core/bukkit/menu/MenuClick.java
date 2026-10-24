package br.com.muxe.core.bukkit.menu;

@FunctionalInterface
public interface MenuClick {

    MenuClick NONE = context -> {
    };

    void click(ClickContext context);
}
