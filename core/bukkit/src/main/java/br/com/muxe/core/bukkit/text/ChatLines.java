package br.com.muxe.core.bukkit.text;

import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ChatLines {

    private ChatLines() {
    }

    public static void send(Player player, List<String> lines, int center, String url) {
        for (String raw : lines) {
            ChatLine line = ChatLine.parse(raw);
            String text = line.render(center);
            if (line.linked() && url != null && !url.isBlank()) {
                BaseComponent[] parts = TextComponent.fromLegacyText(text);
                ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
                for (BaseComponent part : parts) {
                    part.setClickEvent(click);
                }
                player.spigot().sendMessage(parts);
            } else {
                player.sendMessage(text);
            }
        }
    }
}
