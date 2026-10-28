package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.List;
import java.util.Optional;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

public final class TagList {

    private final MessageCatalog messages;
    private final String chatTemplate;

    public TagList(MessageCatalog messages, String chatTemplate) {
        this.messages = messages;
        this.chatTemplate = chatTemplate;
    }

    public List<TagChoice> choices(List<Tag> usable, String player, Optional<Medal> medal) {
        return TagChoice.of(usable, player, medal, chatTemplate, messages.get("tag-preview-message"), messages.lines("tag-hover"));
    }

    public void send(Player player, List<Tag> usable, Optional<Medal> medal) {
        List<TagChoice> choices = choices(usable, player.getName(), medal);
        if (choices.isEmpty()) {
            player.sendMessage(messages.get("tag-none"));
            return;
        }
        player.spigot().sendMessage(components(choices));
    }

    public BaseComponent[] components(List<TagChoice> choices) {
        return ChoiceLine.components(messages.get("tag-list-header"), messages.get("tag-list-separator"), choices);
    }
}
