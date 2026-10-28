package br.com.muxe.core.bukkit.command.framework.scan;

import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionProvider;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;

final class LocalSuggestions {

    private final Map<String, SuggestionProvider> local;
    private final SuggestionRegistry shared;

    private LocalSuggestions(Map<String, SuggestionProvider> local, SuggestionRegistry shared) {
        this.local = local;
        this.shared = shared;
    }

    static LocalSuggestions of(Object instance, SuggestionRegistry shared) {
        Map<String, SuggestionProvider> local = new HashMap<>();
        for (Method method : CommandScanner.methods(instance.getClass())) {
            Suggestions suggestions = method.getAnnotation(Suggestions.class);
            if (suggestions != null) {
                local.put(suggestions.value(), provider(instance, method));
            }
        }
        return new LocalSuggestions(local, shared);
    }

    SuggestionProvider find(String name, Method user) {
        SuggestionProvider provider = local.get(name);
        if (provider != null) {
            return provider;
        }
        return shared.find(name).orElseThrow(() -> new IllegalStateException(
            CommandScanner.describe(user) + " uses the unknown suggestion provider '" + name + "'"));
    }

    private static SuggestionProvider provider(Object instance, Method method) {
        boolean takesSender = method.getParameterCount() == 1 && CommandSender.class.isAssignableFrom(method.getParameterTypes()[0]);
        if (!Collection.class.isAssignableFrom(method.getReturnType()) || (method.getParameterCount() > 0 && !takesSender)) {
            throw new IllegalStateException(CommandScanner.describe(method) + " must return a collection of strings and take nothing or the sender");
        }
        method.trySetAccessible();
        return sender -> {
            if (takesSender && !method.getParameterTypes()[0].isInstance(sender)) {
                return List.of();
            }
            try {
                Collection<?> values = (Collection<?>) (takesSender ? method.invoke(instance, sender) : method.invoke(instance));
                return values == null ? List.of() : values.stream().map(String::valueOf).toList();
            } catch (InvocationTargetException | IllegalAccessException exception) {
                throw new IllegalStateException("Suggestions of " + CommandScanner.describe(method) + " failed", exception);
            }
        };
    }
}
