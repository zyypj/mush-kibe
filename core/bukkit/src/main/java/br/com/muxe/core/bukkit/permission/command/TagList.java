package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
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
        return choices(Language.DEFAULT, usable, player, medal);
    }

    public List<TagChoice> choices(List<Tag> usable, String player, Optional<Medal> medal, UnaryOperator<RankDisplay> style) {
        return choices(Language.DEFAULT, usable, player, medal, style);
    }

    public List<TagChoice> choices(Language language, List<Tag> usable, String player, Optional<Medal> medal) {
        return choices(language, usable, player, medal, UnaryOperator.identity());
    }

    public List<TagChoice> choices(Language language, List<Tag> usable, String player, Optional<Medal> medal, UnaryOperator<RankDisplay> style) {
        return TagChoice.of(usable, player, medal, style, chatTemplate, messages.get(language, "tag-preview-message"), messages.lines(language, "tag-hover"));
    }

    public void send(Player player, List<Tag> usable, Optional<Medal> medal, UnaryOperator<RankDisplay> style) {
        Language language = messages.language(player);
        List<TagChoice> choices = choices(language, usable, player.getName(), medal, style);
        if (choices.isEmpty()) {
            player.sendMessage(messages.get(language, "tag-none"));
            return;
        }
        lines(language, choices).forEach(player.spigot()::sendMessage);
    }

    public List<BaseComponent[]> lines(List<TagChoice> choices) {
        return lines(Language.DEFAULT, choices);
    }

    public List<BaseComponent[]> lines(Language language, List<TagChoice> choices) {
        return ChoiceLine.lines(messages.get(language, "tag-list-header"), messages.get(language, "tag-list-separator"), choices);
    }
}
