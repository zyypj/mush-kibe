package br.com.muxe.core.bukkit.command.framework.argument;

import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.command.CommandSender;

public final class BooleanArgument implements ArgumentType<Boolean> {

    public static final String YES = "sim";
    public static final String NO = "não";

    private static final Set<String> TRUE_WORDS = Set.of(YES, "s", "true", "on");
    private static final Set<String> FALSE_WORDS = Set.of(NO, "nao", "n", "false", "off");

    @Override
    public ArgumentResult<Boolean> parse(CommandSender sender, String input) {
        String word = input.toLowerCase(Locale.ROOT);
        if (TRUE_WORDS.contains(word)) {
            return ArgumentResult.of(true);
        }
        if (FALSE_WORDS.contains(word)) {
            return ArgumentResult.of(false);
        }
        return ArgumentResult.rejected(CommandMessages.INVALID_OPTION, "input", input, "options", YES + ", " + NO);
    }

    @Override
    public Collection<String> suggest(CommandSender sender) {
        return List.of(YES, NO);
    }
}
