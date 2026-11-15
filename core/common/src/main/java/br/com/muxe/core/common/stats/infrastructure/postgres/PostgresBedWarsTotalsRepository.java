package br.com.muxe.core.common.stats.infrastructure.postgres;

import br.com.muxe.core.common.persistence.sql.Column;
import br.com.muxe.core.common.persistence.sql.Row;
import br.com.muxe.core.common.persistence.sql.Select;
import br.com.muxe.core.common.persistence.sql.Sql;
import br.com.muxe.core.common.persistence.sql.Table;
import br.com.muxe.core.common.persistence.sql.type.SqlType;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import br.com.muxe.core.common.stats.domain.BedWarsTotalsRepository;
import br.com.muxe.core.common.stats.domain.ModeTotals;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PostgresBedWarsTotalsRepository implements BedWarsTotalsRepository {

    public static final String TOTAL_PERIOD = "total";
    public static final LocalDate TOTAL_START = LocalDate.of(1970, 1, 1);

    private static final Table STATS = Table.of("bedwars", "player_stats");
    private static final Column<UUID> PLAYER_ID = STATS.column("player_id", SqlType.UUID);
    private static final Column<String> MODE = STATS.column("mode", SqlType.TEXT);
    private static final Column<String> PERIOD = STATS.column("period", SqlType.TEXT);
    private static final Column<LocalDate> PERIOD_START = STATS.column("period_start", SqlType.DATE);
    private static final Column<Long> GAMES = STATS.column("games", SqlType.BIGINT);
    private static final Column<Long> BEDS_BROKEN = STATS.column("beds_broken", SqlType.BIGINT);
    private static final Column<Long> BEDS_LOST = STATS.column("beds_lost", SqlType.BIGINT);
    private static final Column<Long> KILLS = STATS.column("kills", SqlType.BIGINT);
    private static final Column<Long> DEATHS = STATS.column("deaths", SqlType.BIGINT);
    private static final Column<Long> ASSISTS = STATS.column("assists", SqlType.BIGINT);
    private static final Column<Long> FINAL_KILLS = STATS.column("final_kills", SqlType.BIGINT);
    private static final Column<Long> FINAL_DEATHS = STATS.column("final_deaths", SqlType.BIGINT);
    private static final Column<Long> FINAL_ASSISTS = STATS.column("final_assists", SqlType.BIGINT);
    private static final Column<Long> WINS = STATS.column("wins", SqlType.BIGINT);
    private static final Column<Long> LOSSES = STATS.column("losses", SqlType.BIGINT);
    private static final Column<Long> WINSTREAK = STATS.column("winstreak", SqlType.BIGINT);
    private static final Column<Long> BEST_WINSTREAK = STATS.column("best_winstreak", SqlType.BIGINT);
    private static final Column<Long> XP = STATS.column("xp", SqlType.BIGINT);

    private static final Select<ModeRow> FIND = Sql.select(MODE, GAMES, BEDS_BROKEN, BEDS_LOST, KILLS, DEATHS, ASSISTS, FINAL_KILLS, FINAL_DEATHS, FINAL_ASSISTS,
            WINS, LOSSES, WINSTREAK, BEST_WINSTREAK, XP).from(STATS)
        .where(PLAYER_ID.eq().and(PERIOD.eq()).and(PERIOD_START.eq()))
        .map(row -> new ModeRow(row.get(MODE), totals(row)))
        .named("stats.bedwars.totals");

    @Override
    public BedWarsTotals totals(UUID playerId) {
        Map<String, ModeTotals> modes = new HashMap<>();
        for (ModeRow mode : FIND.list(PLAYER_ID.is(playerId), PERIOD.is(TOTAL_PERIOD), PERIOD_START.is(TOTAL_START))) {
            modes.put(mode.mode(), mode.totals());
        }
        return new BedWarsTotals(modes);
    }

    private static ModeTotals totals(Row row) throws SQLException {
        return new ModeTotals(row.get(GAMES), row.get(BEDS_BROKEN), row.get(BEDS_LOST), row.get(KILLS), row.get(DEATHS), row.get(ASSISTS), row.get(FINAL_KILLS),
            row.get(FINAL_DEATHS), row.get(FINAL_ASSISTS), row.get(WINS), row.get(LOSSES), row.get(WINSTREAK), row.get(BEST_WINSTREAK), row.get(XP));
    }

    private record ModeRow(String mode, ModeTotals totals) {
    }
}
