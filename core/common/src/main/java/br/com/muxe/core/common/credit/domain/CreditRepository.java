package br.com.muxe.core.common.credit.domain;

import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

public interface CreditRepository {

    long balance(UUID playerId);

    long add(UUID playerId, long amount, String reason, Instant at);

    OptionalLong spend(UUID playerId, long amount, String reason, Instant at);
}
