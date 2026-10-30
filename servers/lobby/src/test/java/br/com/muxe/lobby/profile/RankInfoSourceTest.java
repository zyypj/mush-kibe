package br.com.muxe.lobby.profile;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.PermissionFixtures;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankInfoSourceTest {

    private final PlayerInfo fallback = new PlayerInfo("&7Membro", 0);

    @Test
    void theScoreboardShowsTheTagThePlayerDisplays() {
        RankInfoSource source = new RankInfoSource(player -> PermissionFixtures.DATA.tag("ultra"), player -> 0L, fallback);

        assertThat(source.of(UUID.randomUUID())).isEqualTo(new PlayerInfo("&dUltra", 0));
    }

    @Test
    void playersWithoutATagShowTheDefault() {
        RankInfoSource source = new RankInfoSource(player -> Optional.empty(), player -> 0L, fallback);

        assertThat(source.of(UUID.randomUUID())).isEqualTo(fallback);
    }

    @Test
    void theScoreboardShowsTheCreditBalance() {
        RankInfoSource source = new RankInfoSource(player -> Optional.empty(), player -> 1_750L, fallback);

        assertThat(source.of(UUID.randomUUID()).credits()).isEqualTo(1_750L);
    }
}
