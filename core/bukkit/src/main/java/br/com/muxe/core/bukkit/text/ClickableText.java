package br.com.muxe.core.bukkit.text;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ClickableText {

    private ClickableText() {
    }

    public static BaseComponent[] line(List<ChatButton> buttons) {
        List<BaseComponent> parts = new ArrayList<>();
        for (ChatButton button : buttons) {
            parts.addAll(List.of(components(button)));
        }
        return parts.toArray(BaseComponent[]::new);
    }

    public static BaseComponent[] components(ChatButton button) {
        BaseComponent[] parts = TextComponent.fromLegacyText(Colors.translate(button.label()));
        HoverEvent hover = button.hover().isEmpty()
            ? null
            : new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Colors.translate(String.join("\n", button.hover()))));
        ClickEvent click = switch (button.action()) {
            case RUN -> new ClickEvent(ClickEvent.Action.RUN_COMMAND, button.command());
            case SUGGEST -> new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, button.command());
            case NONE -> null;
        };
        for (BaseComponent part : parts) {
            part.setHoverEvent(hover);
            part.setClickEvent(click);
        }
        return parts;
    }

    public static void send(Player player, ChatButton... buttons) {
        player.spigot().sendMessage(line(List.of(buttons)));
    }

    public static void send(Player player, List<ChatButton> buttons) {
        player.spigot().sendMessage(line(buttons));
    }
}
