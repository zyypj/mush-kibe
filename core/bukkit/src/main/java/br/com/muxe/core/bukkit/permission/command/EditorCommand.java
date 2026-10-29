package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.text.CenteredText;
import br.com.muxe.core.bukkit.text.ChatLines;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = EditorCommand.NAME, aliases = "editorpermissoes", description = "Opens a session of the web permission editor.")
@Permission(EditorSessions.PERMISSION)
public final class EditorCommand {

    public static final String NAME = "editor";
    public static final UUID CONSOLE = EditorSessions.CONSOLE;

    private final EditorSessions sessions;
    private final Executor async;
    private final String baseUrl;
    private final Replies replies;

    public EditorCommand(EditorSessions sessions, Executor async, String baseUrl, Replies replies) {
        this.sessions = sessions;
        this.async = async;
        this.baseUrl = baseUrl;
        this.replies = replies;
    }

    public static String link(String baseUrl, String token) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/#" + token;
    }

    @Default
    public void open(CommandSender sender) {
        UUID actor = sender instanceof Player player ? player.getUniqueId() : CONSOLE;
        String name = sender.getName();
        replies.then(sender, CompletableFuture.supplyAsync(() -> sessions.open(actor, name), async), token -> {
            String url = link(baseUrl, token);
            String minutes = Long.toString(sessions.duration().toMinutes());
            if (sender instanceof Player player) {
                ChatLines.send(player, replies.messages().lines("editor-opened", "minutes", minutes), CenteredText.DEFAULT_CENTER, url,
                    replies.messages().lines("editor-hover", "url", url));
            } else {
                replies.send(sender, "editor-console", "url", url, "minutes", minutes);
            }
        });
    }
}
