package br.com.muxe.lobby.bossbar;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.bossbar.BossBarFrames;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LobbyBossBarTest {

    private final UUID reader = UUID.randomUUID();
    private final Map<UUID, Language> languages = new HashMap<>();
    private final Localized<BossBarFrames> frames = Localized.of(language -> language == Language.ENGLISH
        ? new BossBarFrames(List.of("&aHello", "&bWorld"), 10)
        : new BossBarFrames(List.of("&aOlá", "&bMundo"), 10));
    private final LobbyBossBar bar = new LobbyBossBar(null, frames, player -> languages.getOrDefault(player, Language.DEFAULT));

    @Test
    void eachReaderSeesTheFrameOfTheirLanguage() {
        UUID english = UUID.randomUUID();
        languages.put(english, Language.ENGLISH);

        assertThat(bar.frame(reader)).isEqualTo("§aOlá");
        assertThat(bar.frame(english)).isEqualTo("§aHello");
    }

    @Test
    void theFramesAdvanceTogetherForEveryLanguage() {
        bar.advance();

        assertThat(bar.frame(reader)).isEqualTo("§bMundo");
        languages.put(reader, Language.ENGLISH);
        assertThat(bar.frame(reader)).isEqualTo("§bWorld");
        assertThat(bar.period()).isEqualTo(10);
    }
}
