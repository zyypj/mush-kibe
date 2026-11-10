package br.com.muxe.auth.flow;

import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.language.domain.Language;
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

    public List<String> challengeLines(Language language) {
        return messages.lines(language, "challenge");
    }

    public List<String> credentialLines(Language language, boolean registered, boolean challengePassed) {
        List<String> lines = new ArrayList<>(messages.lines(language, registered ? "login" : "register"));
        if (challengePassed) {
            lines.addAll(messages.lines(language, "challenge-passed"));
        }
        return lines;
    }

    public List<String> authenticatedLines(Language language, boolean registered) {
        return messages.lines(language, registered ? "logged-in" : "registered");
    }

    public Title credentialsTitle(Language language, boolean registered) {
        return Title.builder()
            .title(messages.get(language, "title"))
            .subtitle(messages.get(language, registered ? "subtitle-login" : "subtitle-register"))
            .timings(TITLE_FADE_IN, titleStayTicks, TITLE_FADE_OUT)
            .build();
    }

    public void challenge(Player player) {
        challengeLines(messages.language(player)).forEach(player::sendMessage);
        challengeReminder(player);
    }

    public void challengeReminder(Player player) {
        ActionBar.send(player, messages.get(player, "challenge-action-bar"));
    }

    public void credentials(Player player, boolean registered, boolean challengePassed) {
        Language language = messages.language(player);
        credentialLines(language, registered, challengePassed).forEach(player::sendMessage);
        credentialsTitle(language, registered).send(player);
    }

    public void credentialsReminder(Player player, boolean registered) {
        Language language = messages.language(player);
        messages.lines(language, registered ? "prompt-login" : "prompt-register").forEach(player::sendMessage);
        credentialsTitle(language, registered).send(player);
    }

    public void authenticated(Player player, boolean registered) {
        Title.clear(player);
        authenticatedLines(messages.language(player), registered).forEach(player::sendMessage);
    }
}
