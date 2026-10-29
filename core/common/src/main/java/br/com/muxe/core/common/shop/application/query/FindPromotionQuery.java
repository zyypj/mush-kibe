package br.com.muxe.core.common.shop.application.query;

import br.com.muxe.core.common.cqrs.query.Query;
import br.com.muxe.core.common.shop.domain.Promotion;
import java.util.Optional;

public record FindPromotionQuery() implements Query<Optional<Promotion>> {
}
