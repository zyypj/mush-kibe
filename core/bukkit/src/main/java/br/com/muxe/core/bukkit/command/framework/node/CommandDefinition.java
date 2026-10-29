package br.com.muxe.core.bukkit.command.framework.node;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CommandDefinition(
    String name,
    List<String> aliases,
    String description,
    Optional<String> usageKey,
    Optional<String> permission,
    CommandNode root,
    Optional<CommandMethod> usage
) {

    public CommandDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(usageKey, "usageKey");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(usage, "usage");
        aliases = List.copyOf(aliases);
    }
}
