package br.com.muxe.core.bukkit.command.framework.argument;

import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import java.util.function.Function;
import org.bukkit.command.CommandSender;

public final class NumberArgument<T extends Number> implements ArgumentType<T> {

    private final Function<String, T> parser;

    private NumberArgument(Function<String, T> parser) {
        this.parser = parser;
    }

    public static NumberArgument<Integer> integers() {
        return new NumberArgument<>(Integer::valueOf);
    }

    public static NumberArgument<Long> longs() {
        return new NumberArgument<>(Long::valueOf);
    }

    public static NumberArgument<Double> doubles() {
        return new NumberArgument<>(NumberArgument::finite);
    }

    @Override
    public ArgumentResult<T> parse(CommandSender sender, String input) {
        try {
            return ArgumentResult.of(parser.apply(input));
        } catch (NumberFormatException exception) {
            return ArgumentResult.rejected(CommandMessages.INVALID_NUMBER, "input", input);
        }
    }

    private static Double finite(String input) {
        double value = Double.parseDouble(input.replace(',', '.'));
        if (!Double.isFinite(value)) {
            throw new NumberFormatException(input);
        }
        return value;
    }
}
