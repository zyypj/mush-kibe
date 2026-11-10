package br.com.muxe.core.proxy.tablist;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.proxy.config.ProxyConfigFile;
import com.velocitypowered.api.proxy.Player;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TabListTest {

    @TempDir
    private Path folder;

    @Test
    void theDefaultTabShowsMuxeOnTopAndTheLinksBelowWithABlankLineAroundThem() {
        TabListSettings settings = TabListSettings.from(ProxyConfigFile.load(folder));

        assertThat(settings.header()).containsExactly("", "&b&lMUXE", "");
        assertThat(settings.footer()).containsExactly(
            "",
            "&eWebsite &bmuxe.com.br",
            "&eLoja &bloja.muxe.com.br",
            "&eDiscord &bmuxe.com.br/discord",
            ""
        );
    }

    @Test
    void everyServerThePlayerJoinsGetsTheHeaderAndFooter() {
        TabListSettings settings = new TabListSettings(List.of("&b&lMUXE"), List.of("&eLoja &bloja.muxe.com.br"));
        List<Component> sent = new ArrayList<>();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
            if (method.getName().equals("sendPlayerListHeaderAndFooter")) {
                sent.add((Component) args[0]);
                sent.add((Component) args[1]);
            }
            return null;
        });

        TabListListener listener = new TabListListener(settings);
        listener.show(player);
        listener.show(player);

        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        assertThat(sent).containsExactly(
            legacy.deserialize("&b&lMUXE"), legacy.deserialize("&eLoja &bloja.muxe.com.br"),
            legacy.deserialize("&b&lMUXE"), legacy.deserialize("&eLoja &bloja.muxe.com.br")
        );
    }

    @Test
    void theDefaultEnglishTabTranslatesTheStoreLink() {
        Localized<TabListSettings> settings = ProxyConfigFile.loadTexts(folder, ProxyConfigFile.load(folder)).map(TabListSettings::from);

        assertThat(settings.in(Language.ENGLISH).header()).containsExactly("", "&b&lMUXE", "");
        assertThat(settings.in(Language.ENGLISH).footer()).contains("&eStore &bloja.muxe.com.br").hasSize(5);
        assertThat(settings.in(Language.PORTUGUESE).footer()).contains("&eLoja &bloja.muxe.com.br");
    }

    @Test
    void eachPlayerSeesTheTabInTheirLanguage() {
        UUID english = UUID.randomUUID();
        UUID portuguese = UUID.randomUUID();
        Localized<TabListSettings> settings = Localized.of(language -> language == Language.ENGLISH
            ? new TabListSettings(List.of("&b&lMUXE"), List.of("&eStore"))
            : new TabListSettings(List.of("&b&lMUXE"), List.of("&eLoja")));
        List<Component> sent = new ArrayList<>();
        TabListListener listener = new TabListListener(settings, player -> player.equals(english) ? Language.ENGLISH : Language.PORTUGUESE);

        listener.show(player(english, sent));
        listener.show(player(portuguese, sent));

        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        assertThat(sent).containsExactly(
            legacy.deserialize("&b&lMUXE"), legacy.deserialize("&eStore"),
            legacy.deserialize("&b&lMUXE"), legacy.deserialize("&eLoja")
        );
    }

    private static Player player(UUID id, List<Component> sent) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
            if (method.getName().equals("getUniqueId")) {
                return id;
            }
            if (method.getName().equals("sendPlayerListHeaderAndFooter")) {
                sent.add((Component) args[0]);
                sent.add((Component) args[1]);
            }
            return null;
        });
    }
}
