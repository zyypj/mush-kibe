package br.com.muxe.core.bukkit.command.framework.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class CommandNode {

    private final String name;
    private final List<String> aliases = new ArrayList<>();
    private final Map<String, CommandNode> children = new TreeMap<>();
    private CommandMethod method;

    public CommandNode(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return List.copyOf(aliases);
    }

    public Collection<CommandNode> children() {
        return List.copyOf(children.values());
    }

    public Optional<CommandMethod> method() {
        return Optional.ofNullable(method);
    }

    public Optional<CommandNode> child(String word) {
        return children.values().stream().filter(child -> child.matches(word)).findFirst();
    }

    public CommandNode childOrCreate(String word) {
        String key = word.toLowerCase(Locale.ROOT);
        CommandNode existing = children.get(key);
        if (existing != null) {
            return existing;
        }
        if (child(key).isPresent()) {
            throw new IllegalStateException("'" + key + "' under '" + name + "' is already an alias");
        }
        CommandNode created = new CommandNode(key);
        children.put(key, created);
        return created;
    }

    public void alias(CommandNode child, String alias) {
        String key = alias.toLowerCase(Locale.ROOT);
        if (child(key).isPresent()) {
            throw new IllegalStateException("'" + key + "' under '" + name + "' is already taken");
        }
        child.aliases.add(key);
    }

    public void attach(CommandMethod handler) {
        if (method != null) {
            throw new IllegalStateException("'" + name + "' already has a handler");
        }
        method = handler;
    }

    public List<CommandMethod> methods() {
        List<CommandMethod> all = new ArrayList<>();
        if (method != null) {
            all.add(method);
        }
        children.values().forEach(child -> all.addAll(child.methods()));
        return all;
    }

    private boolean matches(String word) {
        return name.equalsIgnoreCase(word) || aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(word));
    }
}
