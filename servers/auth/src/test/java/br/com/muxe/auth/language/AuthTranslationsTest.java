package br.com.muxe.auth.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.auth.MuxeAuthPlugin;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTranslationsTest {

    private static final LocalizedConfig CONFIG = LocalizedConfig.bundled(MuxeAuthPlugin.class, "config.yml");

    @Test
    void everyPlayerTextHasAnEnglishTranslation() {
        assertThat(CONFIG.untranslated(Language.ENGLISH, "")).isEmpty();
    }

    @Test
    void kickTextsFollowTheReadersLanguage() {
        UUID english = UUID.randomUUID();
        UUID portuguese = UUID.randomUUID();
        MessageCatalog messages = MessageCatalog.from(CONFIG, "messages")
            .speaking(player -> player.equals(english) ? Language.ENGLISH : Language.PORTUGUESE);

        assertThat(messages.get(english, "too-many-attempts")).isEqualTo("§cToo many wrong attempts. Wait a few minutes and try again.");
        assertThat(messages.get(portuguese, "too-many-attempts")).isEqualTo("§cMuitas tentativas incorretas. Aguarde alguns minutos e tente novamente.");
        assertThat(messages.get(english, "wrong-password", "remaining", "2")).isEqualTo("§cWrong password. Attempts left: §f2");
    }
}
