package br.com.muxe.core.bukkit.permission.command;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public final class ChoiceLine {

    public static final int MAX_JSON_LENGTH = 30000;

    private ChoiceLine() {
    }

    public static List<BaseComponent[]> lines(String header, String separator, List<? extends ChatChoice> choices) {
        List<BaseComponent[]> lines = new ArrayList<>();
        List<BaseComponent> line = new ArrayList<>(List.of(TextComponent.fromLegacyText(header)));
        int length = length(line);
        boolean empty = true;
        BaseComponent[] between = TextComponent.fromLegacyText(separator);
        int betweenLength = length(List.of(between));
        for (ChatChoice choice : choices) {
            List<BaseComponent> parts = parts(choice);
            int partsLength = length(parts);
            if (!empty && length + betweenLength + partsLength > MAX_JSON_LENGTH) {
                lines.add(line.toArray(BaseComponent[]::new));
                line = new ArrayList<>();
                length = 0;
                empty = true;
            }
            if (!empty) {
                line.addAll(List.of(between));
                length += betweenLength;
            }
            line.addAll(parts);
            length += partsLength;
            empty = false;
        }
        lines.add(line.toArray(BaseComponent[]::new));
        return lines;
    }

    private static List<BaseComponent> parts(ChatChoice choice) {
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(String.join("\n", choice.hover())));
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, choice.command());
        List<BaseComponent> parts = new ArrayList<>();
        for (BaseComponent part : TextComponent.fromLegacyText(choice.label())) {
            part.setHoverEvent(hover);
            part.setClickEvent(click);
            parts.add(part);
        }
        return parts;
    }

    private static int length(List<BaseComponent> parts) {
        return ComponentSerializer.toString(parts.toArray(BaseComponent[]::new)).length();
    }
}
