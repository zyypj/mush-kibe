package br.com.muxe.core.bukkit.command.framework.dispatch;

import br.com.muxe.core.bukkit.command.framework.argument.ArgumentResult;
import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import br.com.muxe.core.bukkit.command.framework.node.ArgumentSpec;
import br.com.muxe.core.bukkit.command.framework.node.CommandDefinition;
import br.com.muxe.core.bukkit.command.framework.node.CommandMethod;
import br.com.muxe.core.bukkit.command.framework.node.CommandNode;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;

public final class CommandDispatcher {

    private final MessageCatalog messages;
    private final Logger logger;

    public CommandDispatcher(MessageCatalog messages, Logger logger) {
        this.messages = messages;
        this.logger = logger;
    }

    public void dispatch(CommandDefinition command, CommandSender sender, String label, String[] args) {
        CommandNode node = command.root();
        int index = 0;
        while (index < args.length) {
            Optional<CommandNode> child = node.child(args[index]);
            if (child.isEmpty()) {
                break;
            }
            node = child.get();
            index++;
        }
        List<String> rest = Arrays.asList(args).subList(index, args.length);
        Optional<CommandMethod> method = node.method();
        if (method.isPresent()) {
            run(command, method.get(), sender, label, rest);
        } else {
            fallback(command, node, sender, label);
        }
    }

    private void run(CommandDefinition command, CommandMethod method, CommandSender sender, String label, List<String> rest) {
        if (!method.accepts(sender)) {
            send(sender, method.sender().refusal());
            return;
        }
        if (!method.permits(sender)) {
            send(sender, CommandMessages.NO_PERMISSION);
            return;
        }
        if (!method.fits(rest.size())) {
            usage(command, sender, label, List.of(method));
            return;
        }
        List<Object> values = new ArrayList<>();
        for (int position = 0; position < method.arguments().size(); position++) {
            ArgumentSpec argument = method.arguments().get(position);
            Optional<String> raw = raw(argument, rest, position).or(argument::defaultValue);
            if (raw.isEmpty()) {
                values.add(argument.wrapped() ? Optional.empty() : null);
                continue;
            }
            switch (argument.type().parse(sender, raw.get())) {
                case ArgumentResult.Parsed<?> parsed -> values.add(argument.wrapped() ? Optional.ofNullable(parsed.value()) : parsed.value());
                case ArgumentResult.Rejected<?> rejected -> {
                    send(sender, rejected.messageKey(), rejected.placeholders().toArray(String[]::new));
                    return;
                }
            }
        }
        invoke(method, sender, label, values);
    }

    private void fallback(CommandDefinition command, CommandNode node, CommandSender sender, String label) {
        List<CommandMethod> methods = node.methods();
        List<CommandMethod> accepting = methods.stream().filter(method -> method.accepts(sender)).toList();
        if (accepting.isEmpty()) {
            send(sender, methods.getFirst().sender().refusal());
            return;
        }
        List<CommandMethod> usable = accepting.stream().filter(method -> method.permits(sender)).toList();
        if (usable.isEmpty()) {
            send(sender, CommandMessages.NO_PERMISSION);
            return;
        }
        usage(command, sender, label, usable);
    }

    private void usage(CommandDefinition command, CommandSender sender, String label, List<CommandMethod> methods) {
        if (command.usage().isPresent()) {
            CommandMethod reply = command.usage().get();
            if (reply.accepts(sender)) {
                invoke(reply, sender, label, List.of());
            } else {
                send(sender, reply.sender().refusal());
            }
            return;
        }
        if (command.usageKey().isPresent()) {
            send(sender, command.usageKey().get());
            return;
        }
        if (methods.size() == 1) {
            send(sender, CommandMessages.USAGE, "usage", methods.getFirst().syntax(label));
            return;
        }
        send(sender, CommandMessages.USAGE_HEADER, "command", label);
        methods.forEach(method -> send(sender, CommandMessages.USAGE_LINE, "usage", method.syntax(label)));
    }

    private void invoke(CommandMethod method, CommandSender sender, String label, List<Object> values) {
        try {
            method.invoke(sender, values);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Command /" + label + " failed for " + sender.getName(), exception);
            send(sender, CommandMessages.FAILED);
        }
    }

    private void send(CommandSender sender, String key, String... placeholders) {
        messages.lines(key, placeholders).forEach(sender::sendMessage);
    }

    private static Optional<String> raw(ArgumentSpec argument, List<String> rest, int position) {
        if (position >= rest.size()) {
            return Optional.empty();
        }
        if (argument.greedy()) {
            return Optional.of(String.join(" ", rest.subList(position, rest.size())));
        }
        return Optional.of(rest.get(position));
    }
}
