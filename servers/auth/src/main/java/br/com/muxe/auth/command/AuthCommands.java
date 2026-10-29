package br.com.muxe.auth.command;

import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import java.util.Set;
import org.bukkit.entity.Player;

public final class AuthCommands {

    public static final String LOGIN = "login";
    public static final String LOGIN_ALIAS = "logar";
    public static final String REGISTER = "register";
    public static final String REGISTER_ALIAS = "registrar";
    public static final Set<String> ALLOWED_LABELS = Set.of(LOGIN, LOGIN_ALIAS, REGISTER, REGISTER_ALIAS);

    private final AuthFlow flow;

    public AuthCommands(AuthFlow flow) {
        this.flow = flow;
    }

    @Command(name = LOGIN, aliases = LOGIN_ALIAS, description = "Logs into your account.", usage = "usage-login")
    public void login(Player player, @Arg("senha") String password) {
        flow.login(player, password);
    }

    @Command(name = REGISTER, aliases = REGISTER_ALIAS, description = "Creates your account.", usage = "usage-register")
    public void register(Player player, @Arg("senha") String password, @Arg("senha") String confirmation) {
        flow.register(player, password, confirmation);
    }
}
