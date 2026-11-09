package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.common.chat.ChatChannel;
import br.com.muxe.core.common.chat.ChatChannels;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class ChannelChoices {

    private final ChatChannels channels;
    private final Executor async;
    private final Map<UUID, ChatChannel> chosen = new ConcurrentHashMap<>();

    public ChannelChoices(ChatChannels channels, Executor async) {
        this.channels = channels;
        this.async = async;
    }

    public ChatChannel of(UUID player) {
        return chosen.getOrDefault(player, ChatChannel.DEFAULT);
    }

    public CompletableFuture<Void> load(UUID player) {
        return CompletableFuture.supplyAsync(() -> channels.of(player), async).thenAccept(channel -> chosen.put(player, channel));
    }

    public CompletableFuture<Void> choose(UUID player, ChatChannel channel) {
        chosen.put(player, channel);
        return CompletableFuture.runAsync(() -> channels.choose(player, channel), async);
    }

    public void forget(UUID player) {
        chosen.remove(player);
    }
}
