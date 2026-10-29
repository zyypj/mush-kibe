package br.com.muxe.core.bukkit.command.framework.node;

import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public enum SenderKind {
    ANY(CommandSender.class, CommandMessages.NO_PERMISSION),
    PLAYER(Player.class, CommandMessages.PLAYERS_ONLY),
    CONSOLE(ConsoleCommandSender.class, CommandMessages.CONSOLE_ONLY);

    private final Class<? extends CommandSender> type;
    private final String refusal;

    SenderKind(Class<? extends CommandSender> type, String refusal) {
        this.type = type;
        this.refusal = refusal;
    }

    public static Optional<SenderKind> of(Class<?> type) {
        for (SenderKind kind : values()) {
            if (kind.type == type) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    public boolean accepts(CommandSender sender) {
        return type.isInstance(sender);
    }

    public String refusal() {
        return refusal;
    }
}
