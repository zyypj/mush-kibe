package br.com.muxe.core.common.stats.domain;

import java.util.UUID;

public interface BedWarsTotalsRepository {

    BedWarsTotals totals(UUID playerId);
}
