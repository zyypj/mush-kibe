package br.com.muxe.core.common.chat;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatChannelsTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void channelsAreParsedByTheirNamesWithOrWithoutAccents() {
        assertThat(ChatChannel.parse("default")).hasValue(ChatChannel.DEFAULT);
        assertThat(ChatChannel.parse("padrao")).hasValue(ChatChannel.DEFAULT);
        assertThat(ChatChannel.parse("Padrão")).hasValue(ChatChannel.DEFAULT);
        assertThat(ChatChannel.parse("PARTY")).hasValue(ChatChannel.PARTY);
        assertThat(ChatChannel.parse("clan")).hasValue(ChatChannel.CLAN);
        assertThat(ChatChannel.parse("global")).isEmpty();
        assertThat(ChatChannel.parse(null)).isEmpty();
    }

    @Test
    void theChosenChannelIsRememberedAndDefaultIsTheFallback() {
        MutableClock clock = MutableClock.startingAt("2026-11-09T12:00:00Z");
        ChatChannels channels = new ChatChannels(new InMemoryExpiringStore(clock), Duration.ofDays(7));
        assertThat(channels.of(PLAYER)).isEqualTo(ChatChannel.DEFAULT);

        channels.choose(PLAYER, ChatChannel.PARTY);
        assertThat(channels.of(PLAYER)).isEqualTo(ChatChannel.PARTY);

        channels.choose(PLAYER, ChatChannel.DEFAULT);
        assertThat(channels.of(PLAYER)).isEqualTo(ChatChannel.DEFAULT);

        channels.choose(PLAYER, ChatChannel.PARTY);
        clock.advance(Duration.ofDays(8));
        assertThat(channels.of(PLAYER)).isEqualTo(ChatChannel.DEFAULT);
    }
}
