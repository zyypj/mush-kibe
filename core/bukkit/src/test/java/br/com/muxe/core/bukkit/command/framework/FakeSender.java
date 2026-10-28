package br.com.muxe.core.bukkit.command.framework;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class FakeSender {

    private final String name;
    private final Set<String> permissions;
    private final List<String> messages = new ArrayList<>();

    private FakeSender(String name, Set<String> permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    public static FakeSender named(String name, String... permissions) {
        return new FakeSender(name, Set.of(permissions));
    }

    public Player player() {
        return as(Player.class);
    }

    public ConsoleCommandSender console() {
        return as(ConsoleCommandSender.class);
    }

    public CommandSender sender() {
        return as(CommandSender.class);
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }

    public void clear() {
        messages.clear();
    }

    private <T extends CommandSender> T as(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "sendMessage" -> {
                if (args[0] instanceof String[] lines) {
                    messages.addAll(Arrays.asList(lines));
                } else {
                    messages.add((String) args[0]);
                }
                yield null;
            }
            case "hasPermission" -> args[0] instanceof String permission && permissions.contains(permission);
            case "isOnline", "canSee" -> true;
            case "isOp" -> false;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> type.getSimpleName() + "(" + name + ")";
            default -> method.getReturnType() == boolean.class ? false : null;
        }));
    }
}
