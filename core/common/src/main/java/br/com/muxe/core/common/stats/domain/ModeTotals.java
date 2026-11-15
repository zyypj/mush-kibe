package br.com.muxe.core.common.stats.domain;

public record ModeTotals(
    long games,
    long bedsBroken,
    long bedsLost,
    long kills,
    long deaths,
    long assists,
    long finalKills,
    long finalDeaths,
    long finalAssists,
    long wins,
    long losses,
    long winstreak,
    long bestWinstreak,
    long xp
) {

    public static final ModeTotals ZERO = new ModeTotals(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
}
