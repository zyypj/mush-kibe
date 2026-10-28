package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class TagList {

    private final MessageCatalog messages;
    private final String chatTemplate;

    public TagList(MessageCatalog messages, String chatTemplate) {
        this.messages = messages;
        this.chatTemplate = chatTemplate;
    }

    public List<TagChoice> choices(List<Tag> usable, String player) {
        return TagChoice.of(usable, player, chatTemplate, messages.get("tag-preview-message"), messages.lines("tag-hover"));
    }

    public void send(Player player, List<Tag> usable) {
        List<TagChoice> choices = choices(usable, player.getName());
        if (choices.isEmpty()) {
            player.sendMessage(messages.get("tag-none"));
            return;
        }
        player.spigot().sendMessage(components(choices));
    }

    public BaseComponent[] components(List<TagChoice> choices) {
        List<BaseComponent> parts = new ArrayList<>(List.of(TextComponent.fromLegacyText(messages.get("tag-list-header"))));
        BaseComponent[] separator = TextComponent.fromLegacyText(messages.get("tag-list-separator"));
        for (int index = 0; index < choices.size(); index++) {
            if (index > 0) {
                parts.addAll(List.of(separator));
            }
            TagChoice choice = choices.get(index);
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
