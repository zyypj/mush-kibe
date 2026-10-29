package br.com.muxe.core.bukkit.command.framework.scan;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentType;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionProvider;
import br.com.muxe.core.bukkit.command.framework.node.ArgumentSpec;
import br.com.muxe.core.bukkit.command.framework.node.CommandMethod;
import br.com.muxe.core.bukkit.command.framework.node.SenderKind;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;

final class MethodReader {

    private final Object target;
    private final ArgumentTypes types;
    private final LocalSuggestions suggestions;

    MethodReader(Object target, ArgumentTypes types, LocalSuggestions suggestions) {
        this.target = target;
        this.types = types;
        this.suggestions = suggestions;
    }

    CommandMethod read(Method method, List<String> permissions, String path) {
        Parameter[] parameters = method.getParameters();
        SenderKind sender = SenderKind.ANY;
        int first = 0;
        if (parameters.length > 0 && CommandSender.class.isAssignableFrom(parameters[0].getType())) {
            sender = SenderKind.of(parameters[0].getType()).orElseThrow(() -> new IllegalStateException(
                CommandScanner.describe(method) + " must take a CommandSender, Player or ConsoleCommandSender"));
            first = 1;
        }
        List<ArgumentSpec> arguments = new ArrayList<>();
        for (int index = first; index < parameters.length; index++) {
            ArgumentSpec argument = argument(method, parameters[index]);
            if (!arguments.isEmpty() && arguments.getLast().optional() && !argument.optional()) {
                throw new IllegalStateException(CommandScanner.describe(method) + " has a required argument after an optional one");
            }
            if (argument.greedy() && index != parameters.length - 1) {
                throw new IllegalStateException(CommandScanner.describe(method) + " must have its @Greedy argument last");
            }
            arguments.add(argument);
        }
        method.trySetAccessible();
        return new CommandMethod(target, method, sender, first == 1, arguments, permissions, path);
    }

    private ArgumentSpec argument(Method method, Parameter parameter) {
        boolean wrapped = parameter.getType() == Optional.class;
        Class<?> element = wrapped ? element(method, parameter) : parameter.getType();
        Arg arg = parameter.getAnnotation(Arg.class);
        String name = arg != null && !arg.value().isBlank() ? arg.value() : parameter.getName();
        Optional<String> defaultValue = Optional.ofNullable(arg).map(Arg::defaultValue).filter(value -> !value.equals(Arg.NONE));
        boolean greedy = parameter.isAnnotationPresent(Greedy.class);
        if (greedy && element != String.class) {
            throw new IllegalStateException(CommandScanner.describe(method) + " can only use @Greedy on text");
        }
        ArgumentType<?> type = types.find(element).orElseThrow(() -> new IllegalStateException(
            CommandScanner.describe(method) + " has no argument type for " + element.getName()));
        Suggest suggest = parameter.getAnnotation(Suggest.class);
        List<String> fixed = suggest == null ? List.of() : List.of(suggest.value());
        Optional<SuggestionProvider> provider = Optional.ofNullable(suggest)
            .map(Suggest::provider)
            .filter(value -> !value.isBlank())
            .map(value -> suggestions.find(value, method));
        return new ArgumentSpec(name, type, defaultValue, wrapped || defaultValue.isPresent(), greedy, wrapped, fixed, provider);
    }

    private static Class<?> element(Method method, Parameter parameter) {
        Type type = parameter.getParameterizedType();
        if (type instanceof ParameterizedType generic && generic.getActualTypeArguments()[0] instanceof Class<?> element) {
            return element;
        }
        throw new IllegalStateException(CommandScanner.describe(method) + " must say what its Optional holds");
    }
}
