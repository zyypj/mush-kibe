package br.com.muxe.core.bukkit.command.framework.scan;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.node.CommandDefinition;
import br.com.muxe.core.bukkit.command.framework.node.CommandMethod;
import br.com.muxe.core.bukkit.command.framework.node.CommandNode;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CommandScanner {

    private final ArgumentTypes types;
    private final SuggestionRegistry suggestions;

    public CommandScanner(ArgumentTypes types, SuggestionRegistry suggestions) {
        this.types = types;
        this.suggestions = suggestions;
    }

    public List<CommandDefinition> scan(Object instance) {
        Class<?> type = instance.getClass();
        MethodReader reader = new MethodReader(instance, types, LocalSuggestions.of(instance, suggestions));
        Command group = type.getAnnotation(Command.class);
        if (group != null) {
            return List.of(group(instance, group, reader));
        }
        List<CommandDefinition> found = new ArrayList<>();
        for (Method method : methods(type)) {
            Command single = method.getAnnotation(Command.class);
            if (single != null) {
                found.add(single(single, method, reader));
            }
        }
        if (found.isEmpty()) {
            throw new IllegalArgumentException(type.getName() + " has no @Command");
        }
        return found;
    }

    private CommandDefinition group(Object instance, Command group, MethodReader reader) {
        Class<?> type = instance.getClass();
        Optional<String> permission = permission(type.getAnnotation(Permission.class));
        CommandNode root = new CommandNode(group.name());
        Optional<CommandMethod> usage = Optional.empty();
        for (Method method : methods(type)) {
            Subcommand subcommand = method.getAnnotation(Subcommand.class);
            boolean isDefault = method.isAnnotationPresent(Default.class);
            if (method.isAnnotationPresent(Command.class)) {
                throw new IllegalStateException(describe(method) + " cannot use @Command inside a class annotated with @Command");
            }
            if (subcommand != null && isDefault) {
                throw new IllegalStateException(describe(method) + " cannot be both @Default and @Subcommand");
            }
            List<String> permissions = permissions(permission, method);
            if (subcommand != null) {
                String path = path(subcommand.value(), method);
                CommandNode node = node(root, path, subcommand.aliases());
                node.attach(reader.read(method, permissions, path));
            } else if (isDefault) {
                root.attach(reader.read(method, permissions, ""));
            }
            if (method.isAnnotationPresent(Usage.class)) {
                CommandMethod reply = reader.read(method, permissions, "");
                if (!reply.arguments().isEmpty() || usage.isPresent()) {
                    throw new IllegalStateException(describe(method) + " must be the only @Usage and take only the sender");
                }
                usage = Optional.of(reply);
            }
        }
        if (root.methods().isEmpty()) {
            throw new IllegalStateException(type.getName() + " has no @Default or @Subcommand method");
        }
        return new CommandDefinition(group.name(), List.of(group.aliases()), group.description(), usageKey(group), permission, root, usage);
    }

    private CommandDefinition single(Command command, Method method, MethodReader reader) {
        Optional<String> permission = permission(method.getAnnotation(Permission.class));
        CommandNode root = new CommandNode(command.name());
        root.attach(reader.read(method, permission.stream().toList(), ""));
        return new CommandDefinition(command.name(), List.of(command.aliases()), command.description(), usageKey(command), permission, root, Optional.empty());
    }

    private static CommandNode node(CommandNode root, String path, String[] aliases) {
        CommandNode parent = root;
        CommandNode node = root;
        for (String word : path.split(" ")) {
            parent = node;
            node = node.childOrCreate(word);
        }
        for (String alias : aliases) {
            parent.alias(node, alias);
        }
        return node;
    }

    private static String path(String value, Method method) {
        String path = String.join(" ", value.trim().split("\\s+"));
        if (path.isEmpty()) {
            throw new IllegalStateException(describe(method) + " has an empty @Subcommand; use @Default");
        }
        return path;
    }

    private static List<String> permissions(Optional<String> command, Method method) {
        List<String> all = new ArrayList<>(command.stream().toList());
        permission(method.getAnnotation(Permission.class)).filter(value -> !all.contains(value)).ifPresent(all::add);
        return all;
    }

    private static Optional<String> permission(Permission permission) {
        return Optional.ofNullable(permission).map(Permission::value).filter(value -> !value.isBlank());
    }

    private static Optional<String> usageKey(Command command) {
        return Optional.of(command.usage()).filter(value -> !value.isBlank());
    }

    static List<Method> methods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .sorted(Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount))
            .toList();
    }

    static String describe(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
