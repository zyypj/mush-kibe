package br.com.muxe.core.proxy.motd;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.text.TextWidth;
import br.com.muxe.core.proxy.config.ProxyConfigFile;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MotdTest {

    private static final String TITLE = "&c&m  &6&m  &e&m  &a&m  &b&m  &r &b&lMUXE&r &b&m  &a&m  &e&m  &6&m  &c&m  ";
    private static final String INVITE = "&a&lVENHA JOGAR";

    @TempDir
    private Path folder;

    @Test
    void theDefaultMotdShowsMuxeAndTheInviteCenteredWithRoomForThreeThousand() {
        MotdSettings settings = MotdSettings.from(ProxyConfigFile.load(folder));

        assertThat(settings.maxPlayers()).isEqualTo(3000);
        assertThat(settings.lines()).hasSize(2);
        assertThat(settings.lines().get(0)).endsWith(TITLE).startsWith(" ");
        assertThat(settings.lines().get(1)).endsWith(INVITE).startsWith(" ");
        settings.lines().forEach(line -> assertThat(centerOffset(line)).isLessThanOrEqualTo(TextWidth.SPACE));
        assertThat(PlainTextComponentSerializer.plainText().serialize(settings.description())).contains("MUXE", "\n", "VENHA JOGAR");
    }

    @Test
    void theBreakAndThePaddingOfTheSecondLineAreNeverStruckThrough() {
        MotdSettings settings = MotdSettings.from(ProxyConfigFile.load(folder));

        List<TextComponent> pieces = new ArrayList<>();
        collect(settings.description(), pieces);

        assertThat(pieces).filteredOn(piece -> piece.content().contains("\n")).isNotEmpty()
            .allSatisfy(piece -> assertThat(piece.decoration(TextDecoration.STRIKETHROUGH)).isNotEqualTo(TextDecoration.State.TRUE));
        assertThat(pieces).filteredOn(piece -> piece.content().contains("VENHA JOGAR")).singleElement()
            .satisfies(piece -> assertThat(piece.decoration(TextDecoration.STRIKETHROUGH)).isNotEqualTo(TextDecoration.State.TRUE));
    }

    @Test
    void everyPingGetsTheMotdAndTheMaximumButKeepsTheOnlineCount() {
        MotdSettings settings = new MotdSettings(List.of("&bA", "&aB"), 3000);
        ServerPing original = ServerPing.builder()
            .version(new ServerPing.Version(47, "1.8"))
            .onlinePlayers(1479)
            .maximumPlayers(500)
            .description(Component.text("velocity"))
            .build();
        ProxyPingEvent event = new ProxyPingEvent(connection(), original);

        new MotdListener(settings).onPing(event);

        assertThat(event.getPing().getDescriptionComponent()).isEqualTo(settings.description());
        assertThat(event.getPing().getPlayers().orElseThrow().getMax()).isEqualTo(3000);
        assertThat(event.getPing().getPlayers().orElseThrow().getOnline()).isEqualTo(1479);
    }

    private static void collect(Component component, List<TextComponent> pieces) {
        if (component instanceof TextComponent text) {
            pieces.add(text);
        }
        component.children().forEach(child -> collect(child, pieces));
    }

    private static InboundConnection connection() {
        return (InboundConnection) Proxy.newProxyInstance(InboundConnection.class.getClassLoader(), new Class<?>[] {InboundConnection.class}, (proxy, method, args) -> null);
    }

    private static int centerOffset(String line) {
        String text = line.stripLeading();
        int left = (line.length() - text.length()) * TextWidth.SPACE;
        int right = TextWidth.SERVER_LIST_WIDTH - left - TextWidth.of(text);
        return Math.abs(left - right);
    }
}
