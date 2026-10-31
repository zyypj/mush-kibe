package br.com.muxe.core.bukkit.greeting;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

class LeftoverTitleCleanerTest {

    @Test
    void arrivingOnAServerClearsTheTitleLeftFromThePreviousOne() {
        List<Player> cleared = new CopyOnWriteArrayList<>();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        });

        new LeftoverTitleCleaner(cleared::add).onJoin(new PlayerJoinEvent(player, ""));

        assertThat(cleared).containsExactly(player);
    }

    @Test
    void theTitleIsClearedBeforeAnyOtherPluginSendsItsOwn() throws NoSuchMethodException {
        EventHandler handler = LeftoverTitleCleaner.class.getMethod("onJoin", PlayerJoinEvent.class).getAnnotation(EventHandler.class);

        assertThat(handler.priority()).isEqualTo(EventPriority.LOWEST);
    }
}
