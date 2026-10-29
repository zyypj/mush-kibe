package br.com.muxe.core.bukkit.permission.command;

import static br.com.muxe.core.bukkit.permission.command.RankTestKit.MESSAGES;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.text.ChatLines;
import br.com.muxe.core.common.permission.editor.EditorSessions;
import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EditorCommandTest {

    private static final Pattern LINK = Pattern.compile("http://151\\.244\\.40\\.191:1031/#([A-Za-z0-9_-]+)");

    private final RankTestKit kit = new RankTestKit();
    private final EditorSessions sessions = new EditorSessions(new InMemoryExpiringStore(kit.clock), kit.clock, EditorSessions.DEFAULT_DURATION);

    @AfterEach
    void close() {
        kit.close();
    }

    @Test
    void theConsoleGetsAPlainLinkToItsOwnSession() {
        kit.framework.register(new EditorCommand(sessions, Runnable::run, "http://151.244.40.191:1031/", kit.replies));
        FakeSender console = FakeSender.named("CONSOLE", EditorSessions.PERMISSION);

        kit.framework.run(console.console(), "editor");

        assertThat(console.messages()).singleElement().satisfies(line -> {
            assertThat(line).startsWith("§aEditor de permissões: §fhttp://151.244.40.191:1031/#").endsWith(" §7(expira em 30 minutos)");
            Matcher matcher = LINK.matcher(line);
            assertThat(matcher.find()).isTrue();
            assertThat(sessions.find(matcher.group(1))).hasValueSatisfying(session -> assertThat(session.actorName()).isEqualTo("CONSOLE"));
        });
    }

    @Test
    void onlyTheEditorPermissionOpensASession() {
        kit.framework.register(new EditorCommand(sessions, Runnable::run, "http://151.244.40.191:1031", kit.replies));
        FakeSender player = FakeSender.named("Steve");

        kit.framework.run(player.player(), "editor");

        assertThat(player.messages()).containsExactly(MESSAGES.get("no-permission"));
    }

    @Test
    void playersGetAClickableLinkWithAHover() {
        String url = EditorCommand.link("http://151.244.40.191:1031", "abc");
        BaseComponent[] parts = ChatLines.linked("§a§nClique aqui", url, MESSAGES.lines("editor-hover", "url", url));

        assertThat(url).isEqualTo("http://151.244.40.191:1031/#abc");
        assertThat(TextComponent.toPlainText(parts)).isEqualTo("Clique aqui");
        assertThat(List.of(parts)).allSatisfy(part -> {
            assertThat(part.getClickEvent().getAction()).isEqualTo(ClickEvent.Action.OPEN_URL);
            assertThat(part.getClickEvent().getValue()).isEqualTo(url);
            assertThat(TextComponent.toPlainText(part.getHoverEvent().getValue())).contains(url);
        });
    }
}
