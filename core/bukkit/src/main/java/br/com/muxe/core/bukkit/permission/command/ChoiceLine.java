package br.com.muxe.core.bukkit.permission.command;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class ChoiceLine {

    private ChoiceLine() {
    }

    public static BaseComponent[] components(String header, String separator, List<? extends ChatChoice> choices) {
        List<BaseComponent> parts = new ArrayList<>(List.of(TextComponent.fromLegacyText(header)));
        BaseComponent[] between = TextComponent.fromLegacyText(separator);
        for (int index = 0; index < choices.size(); index++) {
            if (index > 0) {
                parts.addAll(List.of(between));
            }
            ChatChoice choice = choices.get(index);
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(String.join("\n", choice.hover())));
            ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, choice.command());
            for (BaseComponent part : TextComponent.fromLegacyText(choice.label())) {
                part.setHoverEvent(hover);
                part.setClickEvent(click);
                parts.add(part);
            }
        }
        return parts.toArray(BaseComponent[]::new);
    }
}
