package br.com.muxe.core.bukkit.permission.display;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class NameTagsTest {

    private static final UUID PLAYER = new UUID(0, 1);
    private static final UUID OTHER = new UUID(0, 2);
    private static final NameTag RANK = new NameTag("998999-", "§7");
    private static final NameTag TEAM = new NameTag("bw-01-azul", "§9A §9");

    @Test
    void withoutAnOverrideTheRankTagIsUsed() {
        NameTags tags = new NameTags();

        assertThat(tags.resolve(PLAYER, ignored -> RANK)).isEqualTo(RANK);
    }

    @Test
    void anOverrideWinsOnlyForThePlayersItAnswersAndCanBeCleared() {
        NameTags tags = new NameTags();
        tags.override(player -> player.equals(PLAYER) ? Optional.of(TEAM) : Optional.empty());

        assertThat(tags.resolve(PLAYER, ignored -> RANK)).isEqualTo(TEAM);
        assertThat(tags.resolve(OTHER, ignored -> RANK)).isEqualTo(RANK);

        tags.clearOverride();

        assertThat(tags.resolve(PLAYER, ignored -> RANK)).isEqualTo(RANK);
    }

    @Test
    void refreshingAPlayerReachesTheAttachedTeams() {
        NameTags tags = new NameTags();
        List<Player> refreshed = new ArrayList<>();
        tags.attach(refreshed::add);

        tags.refresh(null);

        assertThat(refreshed).hasSize(1);
    }
}
