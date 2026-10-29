package br.com.muxe.core.bukkit.command.framework.argument;

import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class EnumArgument<E extends Enum<E>> implements ArgumentType<E> {

    private final Map<String, E> constants = new LinkedHashMap<>();

    public EnumArgument(Class<E> type) {
        for (E constant : type.getEnumConstants()) {
            constants.put(word(constant.name()), constant);
        }
    }

    @Override
    public ArgumentResult<E> parse(CommandSender sender, String input) {
        E constant = constants.get(word(input));
        if (constant == null) {
            return ArgumentResult.rejected(CommandMessages.INVALID_OPTION, "input", input, "options", String.join(", ", constants.keySet()));
        }
        return ArgumentResult.of(constant);
    }

    @Override
    public Collection<String> suggest(CommandSender sender) {
        return List.copyOf(constants.keySet());
    }

    private static String word(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
