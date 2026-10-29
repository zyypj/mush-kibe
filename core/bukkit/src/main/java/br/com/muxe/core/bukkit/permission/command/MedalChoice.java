package br.com.muxe.core.bukkit.permission.command;

import java.util.List;
import java.util.Objects;

public record MedalChoice(String id, String label, List<String> hover) implements ChatChoice {

    public MedalChoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        hover = List.copyOf(hover);
    }

    @Override
    public String command() {
        return "/" + MedalCommand.NAME + " " + id;
    }
}
