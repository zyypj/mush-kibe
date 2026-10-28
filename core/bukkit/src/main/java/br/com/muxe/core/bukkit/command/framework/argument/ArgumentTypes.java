package br.com.muxe.core.bukkit.command.framework.argument;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public final class ArgumentTypes {

    private static final Map<Class<?>, Class<?>> BOXES = Map.of(
        int.class, Integer.class,
        long.class, Long.class,
        double.class, Double.class,
        boolean.class, Boolean.class
    );

    private final Map<Class<?>, ArgumentType<?>> types = new HashMap<>();

    public static ArgumentTypes basic() {
        return new ArgumentTypes()
            .register(String.class, new StringArgument())
            .register(Integer.class, NumberArgument.integers())
            .register(Long.class, NumberArgument.longs())
            .register(Double.class, NumberArgument.doubles())
            .register(Boolean.class, new BooleanArgument());
    }

    public static ArgumentTypes standard(Server server) {
        return basic().register(Player.class, new PlayerArgument(server));
    }

    public <T> ArgumentTypes register(Class<T> type, ArgumentType<? extends T> argument) {
        types.put(box(type), argument);
        return this;
    }

    public Optional<ArgumentType<?>> find(Class<?> type) {
        Class<?> boxed = box(type);
        ArgumentType<?> found = types.get(boxed);
        if (found != null) {
            return Optional.of(found);
        }
        if (boxed.isEnum()) {
            return Optional.of(enumArgument(boxed));
        }
        return Optional.empty();
    }

    private static Class<?> box(Class<?> type) {
        return BOXES.getOrDefault(type, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentType<?> enumArgument(Class<?> type) {
        return new EnumArgument(type);
    }
}
