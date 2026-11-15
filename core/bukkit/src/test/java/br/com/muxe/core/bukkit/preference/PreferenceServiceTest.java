package br.com.muxe.core.bukkit.preference;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.preference.domain.Preference;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.junit.jupiter.api.Test;

class PreferenceServiceTest {

    private final PreferenceService preferences = new PreferenceService(new SimpleCommandBus(Runnable::run, List.of()), new SimpleQueryBus(Runnable::run, List.of()),
        Logger.getLogger("test"));

    @Test
    void aPlayerWithTheChatOffStopsSeeingOtherPlayersButStillSeesTheirOwnMessages() {
        Player speaker = player();
        Player listening = player();
        Player muted = player();
        preferences.change(muted.getUniqueId(), Preference.CHAT, "false");

        AsyncPlayerChatEvent fromOther = new AsyncPlayerChatEvent(false, speaker, "oi", new HashSet<>(Set.of(speaker, listening, muted)));
        preferences.onChat(fromOther);
        AsyncPlayerChatEvent fromMuted = new AsyncPlayerChatEvent(false, muted, "oi", new HashSet<>(Set.of(speaker, listening, muted)));
        preferences.onChat(fromMuted);

        assertThat(fromOther.getRecipients()).containsExactlyInAnyOrder(speaker, listening);
        assertThat(fromMuted.getRecipients()).containsExactlyInAnyOrder(speaker, listening, muted);
        assertThat(preferences.of(muted.getUniqueId()).on(Preference.CHAT)).isFalse();
        assertThat(preferences.of(listening.getUniqueId()).on(Preference.CHAT)).isTrue();
    }

    private static Player player() {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> id;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "player " + id;
            default -> method.getReturnType() == boolean.class ? false : null;
        });
    }
}
