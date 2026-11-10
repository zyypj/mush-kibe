package br.com.muxe.lobby.profile;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankInfoSourceTest {

    private final PlayerInfo fallback = new PlayerInfo("&7Membro", 0);
    private final Localized<PlayerInfo> fallbacks = Localized.of(language -> language == Language.ENGLISH ? new PlayerInfo("&7Member", 0) : fallback);

    @Test
    void theScoreboardShowsTheTagThePlayerDisplays() {
        RankInfoSource source = new RankInfoSource(player -> PermissionFixtures.DATA.tag("ultra"), player -> 0L, fallbacks, LanguageLookup.DEFAULT);

        assertThat(source.of(UUID.randomUUID())).isEqualTo(new PlayerInfo("&dUltra", 0));
    }

    @Test
    void playersWithoutATagShowTheDefault() {
        RankInfoSource source = new RankInfoSource(player -> Optional.empty(), player -> 0L, fallbacks, LanguageLookup.DEFAULT);

        assertThat(source.of(UUID.randomUUID())).isEqualTo(fallback);
    }

    @Test
    void theScoreboardShowsTheCreditBalance() {
        RankInfoSource source = new RankInfoSource(player -> Optional.empty(), player -> 1_750L, fallbacks, LanguageLookup.DEFAULT);

        assertThat(source.of(UUID.randomUUID()).credits()).isEqualTo(1_750L);
    }

    @Test
    void playersWithoutATagReadTheDefaultInTheirLanguage() {
        UUID english = UUID.randomUUID();
        RankInfoSource source = new RankInfoSource(player -> Optional.empty(), player -> 0L, fallbacks,
            player -> player.equals(english) ? Language.ENGLISH : Language.PORTUGUESE);

        assertThat(source.of(english).tag()).isEqualTo("&7Member");
        assertThat(source.of(UUID.randomUUID()).tag()).isEqualTo("&7Membro");
    }

    @Test
    void aRealTagIsShownTheSameInEveryLanguage() {
        RankInfoSource source = new RankInfoSource(player -> PermissionFixtures.DATA.tag("ultra"), player -> 0L, fallbacks, player -> Language.ENGLISH);

        assertThat(source.of(UUID.randomUUID()).tag()).isEqualTo("&dUltra");
    }
}
