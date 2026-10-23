package br.com.muxe.auth.listener;

import br.com.muxe.auth.flow.AuthFlow;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChallengeChatListener implements Listener {

    private final AuthFlow flow;
    private final Executor mainThread;

    public ChallengeChatListener(AuthFlow flow, Executor mainThread) {
        this.flow = flow;
        this.mainThread = mainThread;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (flow.isInChallenge(player.getUniqueId())) {
            String answer = event.getMessage();
            mainThread.execute(() -> {
                if (player.isOnline()) {
                    flow.answerChallenge(player, answer);
                }
            });
        }
    }
}
