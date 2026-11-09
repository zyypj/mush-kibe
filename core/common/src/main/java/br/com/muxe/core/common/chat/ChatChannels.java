package br.com.muxe.core.common.chat;

import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Duration;
import java.util.UUID;

public final class ChatChannels {

    public static final Duration DEFAULT_DURATION = Duration.ofDays(7);
    private static final String PREFIX = "chat:channel:";

    private final ExpiringStore store;
    private final Duration duration;

    public ChatChannels(ExpiringStore store, Duration duration) {
        this.store = store;
        this.duration = duration;
    }

    public ChatChannel of(UUID player) {
        return store.get(PREFIX + player).flatMap(ChatChannel::parse).orElse(ChatChannel.DEFAULT);
    }

    public void choose(UUID player, ChatChannel channel) {
        if (channel == ChatChannel.DEFAULT) {
            store.delete(PREFIX + player);
            return;
        }
        store.put(PREFIX + player, channel.id(), duration);
    }
}
