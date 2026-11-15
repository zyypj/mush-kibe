package br.com.muxe.minigame.bedwars.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.stats.LevelBadges;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import org.junit.jupiter.api.Test;

class LevelChatTest {

    private final LevelBadges badges = LevelBadges.from(RoomFixtures.CONFIG.getConfigurationSection("levels"));

    @Test
    void theChatShowsTheLevelBadgeOfThePlayer() {
        assertThat(LevelChat.badge(badges, 904, true)).isEqualTo("&4[&c904&l⚝&4]");
    }

    @Test
    void aPlayerWhoHidesTheLevelShowsLevelZeroToEveryone() {
        assertThat(LevelChat.badge(badges, 904, false)).isEqualTo("&7[0&l✫&7]");
    }
}
