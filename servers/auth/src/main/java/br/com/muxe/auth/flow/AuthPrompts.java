package br.com.muxe.auth.flow;

import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import java.util.ArrayList;
import java.util.List;
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

    public List<String> challengeLines() {
        return messages.lines("challenge");
    }

    public List<String> credentialLines(boolean registered, boolean challengePassed) {
        List<String> lines = new ArrayList<>(messages.lines(registered ? "login" : "register"));
        if (challengePassed) {
            lines.addAll(messages.lines("challenge-passed"));
        }
        return lines;
    }

    public List<String> authenticatedLines(boolean registered) {
        return messages.lines(registered ? "logged-in" : "registered");
    }

    public void challenge(Player player) {
        challengeLines().forEach(player::sendMessage);
        challengeReminder(player);
    }

    public void challengeReminder(Player player) {
        ActionBar.send(player, messages.get("challenge-action-bar"));
    }

    public void credentials(Player player, boolean registered, boolean challengePassed) {
        credentialLines(registered, challengePassed).forEach(player::sendMessage);
        sendTitle(player, registered);
    }

    public void credentialsReminder(Player player, boolean registered) {
        messages.lines(registered ? "prompt-login" : "prompt-register").forEach(player::sendMessage);
        sendTitle(player, registered);
    }

    public void authenticated(Player player, boolean registered) {
        Title.clear(player);
        authenticatedLines(registered).forEach(player::sendMessage);
    }

    private void sendTitle(Player player, boolean registered) {
        Title.builder()
            .title(messages.get("title"))
            .subtitle(messages.get(registered ? "subtitle-login" : "subtitle-register"))
            .timings(TITLE_FADE_IN, titleStayTicks, TITLE_FADE_OUT)
            .build()
            .send(player);
    }
}
