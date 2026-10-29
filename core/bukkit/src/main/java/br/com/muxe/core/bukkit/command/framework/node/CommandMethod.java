package br.com.muxe.core.bukkit.command.framework.node;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;

public final class CommandMethod {

    private final Object target;
    private final Method method;
    private final SenderKind sender;
    private final boolean takesSender;
    private final List<ArgumentSpec> arguments;
    private final List<String> permissions;
    private final String path;

    public CommandMethod(Object target, Method method, SenderKind sender, boolean takesSender, List<ArgumentSpec> arguments, List<String> permissions, String path) {
        this.target = target;
        this.method = method;
        this.sender = sender;
        this.takesSender = takesSender;
        this.arguments = List.copyOf(arguments);
        this.permissions = List.copyOf(permissions);
        this.path = path;
    }

    public SenderKind sender() {
        return sender;
    }

    public List<ArgumentSpec> arguments() {
        return arguments;
    }

    public List<String> permissions() {
        return permissions;
    }

    public String path() {
        return path;
    }

    public boolean accepts(CommandSender candidate) {
        return sender.accepts(candidate);
    }

    public boolean permits(CommandSender candidate) {
        return permissions.stream().allMatch(candidate::hasPermission);
    }

    public boolean usableBy(CommandSender candidate) {
        return accepts(candidate) && permits(candidate);
    }

    public boolean fits(int count) {
        long required = arguments.stream().filter(argument -> !argument.optional()).count();
        boolean greedy = !arguments.isEmpty() && arguments.getLast().greedy();
        return count >= required && (greedy || count <= arguments.size());
    }

    public Optional<ArgumentSpec> argumentAt(int index) {
        return index >= 0 && index < arguments.size() ? Optional.of(arguments.get(index)) : Optional.empty();
    }

    public String syntax(String label) {
        StringBuilder syntax = new StringBuilder("/").append(label);
        if (!path.isEmpty()) {
            syntax.append(' ').append(path);
        }
        arguments.forEach(argument -> syntax.append(' ').append(argument.syntax()));
        return syntax.toString();
    }

    public void invoke(CommandSender candidate, List<Object> values) {
        List<Object> parameters = new ArrayList<>(values.size() + 1);
        if (takesSender) {
            parameters.add(candidate);
        }
        parameters.addAll(values);
        try {
            method.invoke(target, parameters.toArray());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot call " + method, exception);
        }
    }
}
