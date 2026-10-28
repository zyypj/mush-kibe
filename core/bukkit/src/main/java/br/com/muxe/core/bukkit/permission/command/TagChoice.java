package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TagChoice(String id, String label, List<String> hover) implements ChatChoice {

    public static final String PREVIEW = "{preview}";

    public TagChoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        hover = List.copyOf(hover);
    }

    public static List<TagChoice> of(List<Tag> usable, String player, Optional<Medal> medal, String chatTemplate, String previewMessage, List<String> hoverLines) {
        return usable.stream()
            .sorted(Comparator.comparingInt(Tag::weight).thenComparing(Tag::id))
            .map(tag -> {
                String preview = Colors.translate(new RankDisplay(Optional.of(tag), medal).preview(chatTemplate, player, previewMessage));
                List<String> hover = hoverLines.stream().map(line -> line.replace(PREVIEW, preview)).toList();
                return new TagChoice(tag.id(), Colors.translate(tag.display()), hover);
            })
            .toList();
    }

    @Override
    public String command() {
        return "/" + TagCommand.NAME + " " + id;
    }
}
