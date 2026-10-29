package br.com.muxe.core.bukkit.shop.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.shop.ShopModule;
import br.com.muxe.core.common.shop.domain.Promotion;
import br.com.muxe.core.common.shop.domain.PromotionRepository;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ShopPromotionTest {

    private static final Promotion DEFAULTS = new Promotion("enderlore", List.of("&b&lLOJA", "&eClique para comprar!"));

    private final TestNetwork network = new TestNetwork();
    private final StoredPromotion database = new StoredPromotion();

    @AfterEach
    void close() {
        network.close();
    }

    @Test
    void theConfigPromotionIsUsedUntilSomeoneChangesIt() {
        assertThat(server("lobby-1", "lobby").current()).isEqualTo(DEFAULTS);
    }

    @Test
    void aChangeReachesEveryServerOfTheNetwork() {
        ShopPromotion lobby = server("lobby-1", "lobby");
        ShopPromotion skywars = server("skywars-1", "skywars");
        List<Promotion> seenBySkywars = new ArrayList<>();
        skywars.onChange(seenBySkywars::add);
        Promotion vip = DEFAULTS.featuring("vip").replacing(1, "&a&lVIP");

        assertThat(lobby.change(vip).join().isSuccess()).isTrue();

        assertThat(lobby.current()).isEqualTo(vip);
        assertThat(skywars.current()).isEqualTo(vip);
        assertThat(seenBySkywars).containsExactly(DEFAULTS, vip);
    }

    @Test
    void aServerThatStartsLaterLoadsTheStoredPromotion() {
        Promotion vip = DEFAULTS.featuring("vip");
        server("lobby-1", "lobby").change(vip).join();

        assertThat(server("lobby-2", "lobby").current()).isEqualTo(vip);
    }

    private ShopPromotion server(String id, String group) {
        TestNetwork.Node node = network.node(id, group, ServerKind.LOBBY).start();
        SimpleCommandBus commands = new SimpleCommandBus(Runnable::run, List.of());
        SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
        ShopModule.register(commands, queries, database, new DirectTransactions(), network.clock());
        ShopPromotion promotion = new ShopPromotion(DEFAULTS, commands, queries, node.bus(), Runnable::run, Logger.getLogger("test"));
        promotion.start().join();
        return promotion;
    }

    private static final class StoredPromotion implements PromotionRepository {

        private Promotion promotion;

        @Override
        public Optional<Promotion> find() {
            return Optional.ofNullable(promotion);
        }

        @Override
        public void save(Promotion value, Instant changedAt) {
            promotion = value;
        }
    }
}
