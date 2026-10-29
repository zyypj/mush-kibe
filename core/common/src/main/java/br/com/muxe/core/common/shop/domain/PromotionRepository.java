package br.com.muxe.core.common.shop.domain;

import java.time.Instant;
import java.util.Optional;

public interface PromotionRepository {

    Optional<Promotion> find();

    void save(Promotion promotion, Instant changedAt);
}
