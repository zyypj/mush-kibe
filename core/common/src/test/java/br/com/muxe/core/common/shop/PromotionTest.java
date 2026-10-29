package br.com.muxe.core.common.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.shop.application.command.ChangePromotionCommand;
import br.com.muxe.core.common.shop.application.command.ChangePromotionHandler;
import br.com.muxe.core.common.shop.application.query.FindPromotionHandler;
import br.com.muxe.core.common.shop.application.query.FindPromotionQuery;
import br.com.muxe.core.common.shop.domain.Promotion;
import br.com.muxe.core.common.shop.domain.PromotionRepository;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PromotionTest {

    private final MutableClock clock = MutableClock.startingAt("2026-10-26T12:00:00Z");
    private final DirectTransactions transactions = new DirectTransactions();
    private final StoredPromotion repository = new StoredPromotion();

    @Test
    void linesAreNumberedFromOne() {
        Promotion promotion = new Promotion("enderlore", List.of("&b&lLOJA", "&eClique"));

        Promotion changed = promotion.featuring("vip").replacing(2, "&aR$ 39,99").adding("&eClique para comprar!").without(1);

        assertThat(changed).isEqualTo(new Promotion("vip", List.of("&aR$ 39,99", "&eClique para comprar!")));
        assertThatThrownBy(() -> changed.replacing(3, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Promotion(" ", List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thePromotionIsStoredWithTheTimeItChanged() {
        Promotion promotion = new Promotion("vip", List.of("&aVIP"));

        new ChangePromotionHandler(repository, transactions, clock).handle(new ChangePromotionCommand(promotion));

        assertThat(new FindPromotionHandler(repository, transactions).handle(new FindPromotionQuery())).contains(promotion);
        assertThat(repository.changedAt).isEqualTo(clock.instant());
        assertThat(transactions.operations()).containsExactly("shop.change_promotion", "shop.find_promotion");
    }

    @Test
    void thereIsNoPromotionUntilSomeoneChangesIt() {
        assertThat(new FindPromotionHandler(repository, transactions).handle(new FindPromotionQuery())).isEmpty();
    }

    private static final class StoredPromotion implements PromotionRepository {

        private Promotion promotion;
        private Instant changedAt;

        @Override
        public Optional<Promotion> find() {
            return Optional.ofNullable(promotion);
        }

        @Override
        public void save(Promotion value, Instant at) {
            promotion = value;
            changedAt = at;
        }
    }
}
