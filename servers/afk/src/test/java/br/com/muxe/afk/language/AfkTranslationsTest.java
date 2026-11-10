package br.com.muxe.afk.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.afk.MuxeAfkPlugin;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import org.junit.jupiter.api.Test;

class AfkTranslationsTest {

    private static final LocalizedConfig CONFIG = LocalizedConfig.bundled(MuxeAfkPlugin.class, "config.yml");

    @Test
    void everyPlayerTextHasAnEnglishTranslation() {
        assertThat(CONFIG.untranslated(Language.ENGLISH, "")).isEmpty();
    }

    @Test
    void theArrivalIsShownInTheReadersLanguage() {
        MessageCatalog messages = MessageCatalog.from(CONFIG, "messages");

        assertThat(messages.get(Language.PORTUGUESE, "title")).isEqualTo("§c§lVOCÊ ESTÁ AFK");
        assertThat(messages.get(Language.ENGLISH, "title")).isEqualTo("§c§lYOU ARE AFK");
        assertThat(messages.lines(Language.ENGLISH, "arrived")).containsExactly("§c§lYOU ARE AFK!", "§7Move to go back to the lobby.");
    }
}
