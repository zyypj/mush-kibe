package br.com.muxe.core.bukkit.command.framework.completion;

import br.com.muxe.core.bukkit.command.framework.argument.PlayerArgument;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Server;

public final class SuggestionRegistry {

    public static final String PLAYERS = "players";

    private final Map<String, SuggestionProvider> providers = new HashMap<>();

    public static SuggestionRegistry standard(Server server) {
        return new SuggestionRegistry().register(PLAYERS, new PlayerArgument(server)::suggest);
    }

    public SuggestionRegistry register(String name, SuggestionProvider provider) {
        providers.put(name, provider);
        return this;
    }

    public Optional<SuggestionProvider> find(String name) {
        return Optional.ofNullable(providers.get(name));
    }
}
