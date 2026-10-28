package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.permission.domain.Medal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

public final class MedalList {

    private final MessageCatalog messages;
    private final String chatTemplate;

    public MedalList(MessageCatalog messages, String chatTemplate) {
        this.messages = messages;
        this.chatTemplate = chatTemplate;
    }

    public static String label(Medal medal) {
        return Colors.translate(medal.display() + " " + medal.color() + medal.name());
    }

    public List<MedalChoice> choices(List<Medal> usable, RankDisplay current, String player) {
        List<MedalChoice> choices = new ArrayList<>();
        choices.add(new MedalChoice(MedalCommand.NONE, messages.get("medal-list-none"),
            hover("medal-none-hover", current.withMedal(Optional.empty()), player, "")));
        usable.stream()
            .sorted(Comparator.comparingInt(Medal::weight).thenComparing(Medal::id))
            .forEach(medal -> choices.add(new MedalChoice(medal.id(), Colors.translate(medal.display()),
                hover("medal-hover", current.withMedal(Optional.of(medal)), player, Colors.translate(medal.color() + medal.name())))));
        return List.copyOf(choices);
    }

    public void send(Player player, List<Medal> usable, RankDisplay current) {
        player.spigot().sendMessage(components(choices(usable, current, player.getName())));
    }

    public BaseComponent[] components(List<MedalChoice> choices) {
        return ChoiceLine.components(messages.get("medal-list-header"), messages.get("medal-list-separator"), choices);
    }

    private List<String> hover(String key, RankDisplay display, String player, String name) {
        String preview = Colors.translate(display.preview(chatTemplate, player, messages.get("medal-preview-message")));
        return messages.lines(key, "name", name, "preview", preview);
    }
}
