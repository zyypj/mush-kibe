package br.com.muxe.auth.flow;

import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import org.bukkit.entity.Player;

public final class AuthPrompts {

    private static final int TITLE_FADE_IN = 0;
    private static final int TITLE_FADE_OUT = 10;

    private final MessageCatalog messages;
    private final int titleStayTicks;

    public AuthPrompts(MessageCatalog messages, int titleStayTicks) {
        this.messages = messages;
        this.titleStayTicks = titleStayTicks;
    }

    public void challenge(Player player) {
        player.sendMessage(messages.get("challenge-header"));
        player.sendMessage(messages.get("challenge-instruction"));
        player.sendMessage(messages.get("challenge-hint"));
        challengeReminder(player);
    }

    public void challengeReminder(Player player) {
        ActionBar.send(player, messages.get("challenge-action-bar"));
    }

    public void credentials(Player player, boolean registered) {
        player.sendMessage(messages.get("password-warning"));
        credentialsReminder(player, registered);
    }

    public void credentialsReminder(Player player, boolean registered) {
        player.sendMessage(messages.get(registered ? "prompt-login" : "prompt-register"));
        Title.builder()
            .title(messages.get("title"))
            .subtitle(messages.get(registered ? "subtitle-login" : "subtitle-register"))
            .timings(TITLE_FADE_IN, titleStayTicks, TITLE_FADE_OUT)
            .build()
            .send(player);
    }

    public void authenticated(Player player, boolean registered) {
        Title.clear(player);
        player.sendMessage(messages.get(registered ? "logged-in" : "registered"));
        player.sendMessage(messages.get("sending-to-lobby"));
    }
}
