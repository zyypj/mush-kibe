package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Lifetime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record NodeOptions(boolean value, Lifetime lifetime, Contexts contexts) {

    private static final List<String> TRUE_WORDS = List.of("true", "sim", "s", "on", "verdadeiro");
    private static final List<String> FALSE_WORDS = List.of("false", "nao", "não", "n", "off", "falso");
    private static final String SEPARATOR = "=";

    public NodeOptions {
        Objects.requireNonNull(lifetime, "lifetime");
        Objects.requireNonNull(contexts, "contexts");
    }

    public static Parsed parse(List<Optional<String>> words) {
        boolean value = true;
        Lifetime lifetime = Lifetime.PERMANENT;
        Map<String, String> contexts = new TreeMap<>();
        for (Optional<String> slot : words) {
            if (slot.isEmpty()) {
                continue;
            }
            String word = slot.get().trim().toLowerCase(Locale.ROOT);
            if (TRUE_WORDS.contains(word)) {
                value = true;
            } else if (FALSE_WORDS.contains(word)) {
                value = false;
            } else if (word.contains(SEPARATOR)) {
                int at = word.indexOf(SEPARATOR);
                contexts.put(word.substring(0, at), word.substring(at + 1));
            } else {
                Optional<Lifetime> parsed = Lifetime.parse(word);
                if (parsed.isEmpty()) {
                    return new Parsed(Optional.empty(), Optional.of(slot.get()));
                }
                lifetime = parsed.get();
            }
        }
        try {
            return new Parsed(Optional.of(new NodeOptions(value, lifetime, new Contexts(contexts))), Optional.empty());
        } catch (IllegalArgumentException invalidContext) {
            return new Parsed(Optional.empty(), Optional.of(contexts.toString()));
        }
    }

    public static Optional<Contexts> contexts(List<Optional<String>> words) {
        Parsed parsed = parse(words);
        return parsed.options().filter(options -> options.lifetime().isPermanent() && options.value()).map(NodeOptions::contexts);
    }

    public record Parsed(Optional<NodeOptions> options, Optional<String> rejected) {
    }
}
