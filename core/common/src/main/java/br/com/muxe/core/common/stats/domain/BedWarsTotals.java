package br.com.muxe.core.common.stats.domain;

import java.util.Map;

public record BedWarsTotals(Map<String, ModeTotals> modes) {

    public static final String GENERAL = "geral";
    public static final BedWarsTotals EMPTY = new BedWarsTotals(Map.of());

    public BedWarsTotals {
        modes = Map.copyOf(modes);
    }

    public ModeTotals of(String mode) {
        return modes.getOrDefault(mode, ModeTotals.ZERO);
    }

    public ModeTotals general() {
        return of(GENERAL);
    }
}
