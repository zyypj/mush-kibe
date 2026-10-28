package br.com.muxe.core.bukkit.command.framework.node;

import br.com.muxe.core.bukkit.command.framework.argument.ArgumentType;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.command.CommandSender;

public record ArgumentSpec(
    String name,
    ArgumentType<?> type,
    Optional<String> defaultValue,
    boolean optional,
    boolean greedy,
    boolean wrapped,
    List<String> fixedSuggestions,
    Optional<SuggestionProvider> provider
) {

    public ArgumentSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultValue, "defaultValue");
        Objects.requireNonNull(provider, "provider");
        fixedSuggestions = List.copyOf(fixedSuggestions);
    }

    public String syntax() {
        String shown = greedy ? name + "..." : name;
        return optional ? "[" + shown + "]" : "<" + shown + ">";
    }

    public Collection<String> suggestions(CommandSender sender) {
        if (fixedSuggestions.isEmpty() && provider.isEmpty()) {
            return type.suggest(sender);
        }
        List<String> all = new ArrayList<>(fixedSuggestions);
        provider.ifPresent(source -> all.addAll(source.suggest(sender)));
        return all;
    }
}
