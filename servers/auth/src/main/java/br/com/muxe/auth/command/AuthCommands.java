package br.com.muxe.auth.command;

import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AuthCommands implements CommandExecutor {

    public static final String LOGIN = "login";
    public static final String REGISTER = "register";
    public static final Set<String> ALLOWED_LABELS = Set.of(LOGIN, "logar", REGISTER, "registrar");

    private final AuthFlow flow;
    private final MessageCatalog messages;

    public AuthCommands(AuthFlow flow, MessageCatalog messages) {
        this.flow = flow;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        if (command.getName().equals(LOGIN)) {
            if (args.length != 1) {
                player.sendMessage(messages.get("usage-login"));
                return true;
            }
            flow.login(player, args[0]);
            return true;
        }
        if (command.getName().equals(REGISTER)) {
            if (args.length != 2) {
                player.sendMessage(messages.get("usage-register"));
                return true;
            }
            flow.register(player, args[0], args[1]);
            return true;
        }
        return false;
    }
}
