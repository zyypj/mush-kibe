package br.com.muxe.auth.flow;

import br.com.muxe.auth.session.AuthSessionRegistry;
import br.com.muxe.auth.session.AuthState;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AuthReminder implements Runnable {

    public static final long PERIOD_TICKS = 20L;

    private final AuthSessionRegistry sessions;
    private final AuthPrompts prompts;
    private final long credentialsEverySeconds;
    private long seconds;

    public AuthReminder(AuthSessionRegistry sessions, AuthPrompts prompts, long credentialsEverySeconds) {
        this.sessions = sessions;
        this.prompts = prompts;
        this.credentialsEverySeconds = Math.max(1, credentialsEverySeconds);
    }

    @Override
    public void run() {
        seconds++;
        boolean remindCredentials = seconds % credentialsEverySeconds == 0;
        for (Map.Entry<UUID, AuthState> entry : sessions.snapshot().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            switch (entry.getValue()) {
                case CHALLENGE -> prompts.challengeReminder(player);
                case AWAITING_LOGIN -> {
                    if (remindCredentials) {
                        prompts.credentialsReminder(player, true);
                    }
                }
                case AWAITING_REGISTER -> {
                    if (remindCredentials) {
                        prompts.credentialsReminder(player, false);
                    }
                }
                case LOADING, PROCESSING, AUTHENTICATED -> {
                }
            }
        }
    }
}
