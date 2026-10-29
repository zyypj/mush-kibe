package br.com.muxe.core.bukkit.text;

import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ChatLines {

    private ChatLines() {
    }

    public static void send(Player player, List<String> lines, int center, String url) {
        send(player, lines, center, url, List.of());
    }

    public static void send(Player player, List<String> lines, int center, String url, List<String> hover) {
        for (String raw : lines) {
            ChatLine line = ChatLine.parse(raw);
            String text = line.render(center);
            if (line.linked() && url != null && !url.isBlank()) {
                player.spigot().sendMessage(linked(text, url, hover));
            } else {
                player.sendMessage(text);
            }
        }
    }

    public static BaseComponent[] linked(String text, String url, List<String> hover) {
        BaseComponent[] parts = TextComponent.fromLegacyText(text);
        ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        HoverEvent shown = hover.isEmpty() ? null : new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(String.join("\n", hover)));
        for (BaseComponent part : parts) {
            part.setClickEvent(click);
            if (shown != null) {
                part.setHoverEvent(shown);
            }
        }
        return parts;
    }
}
