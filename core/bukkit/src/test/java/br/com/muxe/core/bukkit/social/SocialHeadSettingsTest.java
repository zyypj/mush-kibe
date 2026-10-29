package br.com.muxe.core.bukkit.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.item.SkullTexture;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SocialHeadSettingsTest {

    private static final Instant MINUTE_ZERO = Instant.parse("2026-10-27T15:00:00Z");

    private final SocialHeadSettings settings = SocialHeadSettings.from(ShopFixtures.CONFIG.getConfigurationSection("social-heads"));

    @Test
    void theHeadShowsFiveNetworksAndChangesEveryMinute() {
        assertThat(settings.interval()).isEqualTo(Duration.ofMinutes(1));
        assertThat(settings.networks()).extracting(SocialNetwork::id).containsExactly("instagram", "twitch", "tiktok", "twitter", "youtube");
    }

    @Test
    void theNetworkFollowsTheClockSoEveryServerShowsTheSameOne() {
        int first = settings.indexAt(MINUTE_ZERO);

        assertThat(settings.indexAt(MINUTE_ZERO.plusSeconds(59))).isEqualTo(first);
        assertThat(settings.indexAt(MINUTE_ZERO.plusSeconds(60))).isEqualTo((first + 1) % 5);
        assertThat(settings.indexAt(MINUTE_ZERO.plusSeconds(5 * 60))).isEqualTo(first);
    }

    @Test
    void eachNetworkHasItsColorLabelAndMessage() {
        SocialNetwork instagram = settings.networks().getFirst();
        SocialNetwork twitch = settings.networks().get(1);
        SocialNetwork youtube = settings.networks().getLast();

        assertThat(instagram.label()).isEqualTo("&d@muxe");
        assertThat(instagram.message()).containsExactly(" ", "&d&lINSTAGRAM:", "&fhttps://www.instagram.com/muxe/", " ");
        assertThat(twitch.message().get(1)).isEqualTo("&5&lTWITCH:");
        assertThat(twitch.texture()).contains(SkullTexture.value("8e8e38b532051fce3b3b7212f4ed1dd4b52ed976abdd286d9132c5b3a54db123"));
        assertThat(youtube.label()).isEqualTo("&c@muxe");
        assertThat(instagram.texture()).contains(SkullTexture.value("90d464186e1a50ddaa14be2259611a4e8458515a5367a9389a9cc7c89c94a93b"));
    }

    @Test
    void everyNetworkShowsItsLogoSoTheHeadIsNeverTheDefaultPlayer() {
        assertThat(settings.networks()).allSatisfy(network -> assertThat(network.texture()).as(network.id()).isPresent());
        assertThat(settings.networks().get(3).texture()).contains(SkullTexture.value("6ad46a422ae59603fd889c25344ff67bc843af8ee518932c2e2ad07cdbf939b3"));
    }

    @Test
    void theLabelFloatsClearOfTheHead() {
        assertThat(settings.labelHeight()).isEqualTo(SocialHeadSettings.DEFAULT_LABEL_HEIGHT).isEqualTo(0.9);
    }

    @Test
    void theIntervalMustBePositiveAndThereMustBeANetwork() {
        SocialNetwork network = new SocialNetwork("youtube", "&c@muxe", Optional.empty(), List.of());

        assertThatThrownBy(() -> new SocialHeadSettings(Duration.ZERO, 0.5, List.of(network))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SocialHeadSettings(Duration.ofMinutes(1), 0.5, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
