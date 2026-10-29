package br.com.muxe.core.bukkit.command.framework.argument;

import org.bukkit.command.CommandSender;

public final class StringArgument implements ArgumentType<String> {

    @Override
    public ArgumentResult<String> parse(CommandSender sender, String input) {
        return ArgumentResult.of(input);
    }
}
