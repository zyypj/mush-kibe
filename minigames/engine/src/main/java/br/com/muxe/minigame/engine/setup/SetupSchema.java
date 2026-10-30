package br.com.muxe.minigame.engine.setup;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface SetupSchema {

    List<String> modes();

    TeamRule teams(String mode);

    List<SetupStep> steps(String mode);

    default Optional<String> modeFromName(String arenaName) {
        int separator = arenaName.indexOf('_');
        if (separator <= 0) {
            return Optional.empty();
        }
        String prefix = arenaName.substring(0, separator).toLowerCase(Locale.ROOT);
        return modes().contains(prefix) ? Optional.of(prefix) : Optional.empty();
    }

    default String displayNameFromName(String arenaName) {
        int separator = arenaName.indexOf('_');
        String base = separator >= 0 && separator + 1 < arenaName.length() ? arenaName.substring(separator + 1) : arenaName;
        StringBuilder name = new StringBuilder();
        for (String word : base.split("[_-]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return name.toString();
    }

    default Optional<SetupStep> step(String mode, String id) {
        return steps(mode).stream().filter(step -> step.id().equals(id)).findFirst();
    }
}
