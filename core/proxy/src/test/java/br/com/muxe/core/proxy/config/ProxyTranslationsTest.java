package br.com.muxe.core.proxy.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.proxy.language.LocalizedConfig;
import br.com.muxe.core.proxy.text.ProxyMessages;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProxyTranslationsTest {

    private static final List<String> PLAYER_FACING_SECTIONS = List.of("messages", "tab-list", "motd");

    @TempDir
    private Path folder;

    @Test
    void everyPlayerFacingPortugueseTextHasAnEnglishText() {
        LocalizedConfig texts = ProxyConfigFile.loadTexts(folder, ProxyConfigFile.bundled(ProxyConfigFile.FILE_NAME));

        for (String section : PLAYER_FACING_SECTIONS) {
            assertThat(texts.untranslated(Language.ENGLISH, section)).as(section).isEmpty();
        }
    }

    @Test
    void theCheckNoticesAMissingEnglishText() {
        LocalizedConfig texts = LocalizedConfig.standardOnly(ProxyConfigFile.bundled(ProxyConfigFile.FILE_NAME));

        assertThat(texts.untranslated(Language.ENGLISH, "messages")).contains("messages.no-server", "messages.no-auth-server");
        assertThat(texts.untranslated(Language.ENGLISH, "tab-list")).containsExactly("tab-list.header", "tab-list.footer");
    }

    @Test
    void theEnglishFileIsWrittenOnFirstStart() {
        ProxyConfigFile.loadTexts(folder, ProxyConfigFile.load(folder));

        assertThat(folder.resolve(LocalizedConfig.resource(Language.ENGLISH))).exists();
    }

    @Test
    void aTextMissingFromTheServerEnglishFileComesFromTheJar() throws Exception {
        Path english = folder.resolve(LocalizedConfig.resource(Language.ENGLISH));
        Files.createDirectories(english.getParent());
        Files.writeString(english, String.join("\n",
            "messages:",
            "  must-authenticate: \"&cLog in first.\"",
            ""
        ), StandardCharsets.UTF_8);

        ProxyMessages messages = new ProxyMessages(ProxyConfigFile.loadTexts(folder, ProxyConfigFile.load(folder)), player -> Language.ENGLISH);

        assertThat(plain(messages.get(Language.ENGLISH, "must-authenticate"))).isEqualTo("Log in first.");
        assertThat(plain(messages.get(Language.ENGLISH, "no-server", "destination", "bedwars"))).isEqualTo("No server available for bedwars.");
        assertThat(plain(messages.get(Language.PORTUGUESE, "no-server", "destination", "bedwars"))).isEqualTo("Nenhum servidor disponível para bedwars.");
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
