package br.com.muxe.core.common.ultraplus.infrastructure.postgres;

import br.com.muxe.core.common.persistence.sql.Column;
import br.com.muxe.core.common.persistence.sql.Select;
import br.com.muxe.core.common.persistence.sql.Sql;
import br.com.muxe.core.common.persistence.sql.Table;
import br.com.muxe.core.common.persistence.sql.Write;
import br.com.muxe.core.common.persistence.sql.type.SqlType;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusRepository;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

public final class PostgresUltraPlusRepository implements UltraPlusRepository {

    private static final Table PLAYERS = Table.of("core", "ultra_plus_players");
    private static final Column<UUID> PLAYER_ID = PLAYERS.column("player_id", SqlType.UUID);
    private static final Column<Integer> GRANTED_DAYS = PLAYERS.column("granted_days", SqlType.INTEGER);
    private static final Column<Optional<String>> EMBLEM = PLAYERS.column("emblem", SqlType.TEXT.optional());
    private static final Column<Optional<String>> PLUS_COLOR = PLAYERS.column("plus_color", SqlType.TEXT.optional());
    private static final Column<Integer> TAG_LEVEL = PLAYERS.column("tag_level", SqlType.SMALLINT);
    private static final Column<Optional<String>> REWARD_MONTH = PLAYERS.column("reward_month", SqlType.TEXT.optional());
    private static final Column<Instant> UPDATED_AT = PLAYERS.column("updated_at", SqlType.TIMESTAMPTZ);

    private static final Select<UltraPlusProfile> FIND = Sql.select(PLAYER_ID, GRANTED_DAYS, EMBLEM, PLUS_COLOR, TAG_LEVEL, REWARD_MONTH).from(PLAYERS)
        .where(PLAYER_ID.eq())
        .map(row -> new UltraPlusProfile(row.get(PLAYER_ID), row.get(GRANTED_DAYS),
            new UltraPlusStyle(row.get(EMBLEM), row.get(PLUS_COLOR), Math.max(UltraPlusStyle.FIRST_LEVEL, row.get(TAG_LEVEL))),
            row.get(REWARD_MONTH).map(YearMonth::parse)))
        .named("ultraplus.find");

    private static final Write<Void> SAVE = Sql.<Void>insertInto(PLAYERS)
        .value(PLAYER_ID)
        .value(GRANTED_DAYS)
        .value(EMBLEM)
        .value(PLUS_COLOR)
        .value(TAG_LEVEL)
        .value(REWARD_MONTH)
        .value(UPDATED_AT)
        .onConflict(PLAYER_ID).doUpdate(GRANTED_DAYS, EMBLEM, PLUS_COLOR, TAG_LEVEL, REWARD_MONTH, UPDATED_AT)
        .named("ultraplus.save");

    @Override
    public Optional<UltraPlusProfile> find(UUID playerId) {
        return FIND.first(PLAYER_ID.is(playerId));
    }

    @Override
    public void save(UltraPlusProfile profile, Instant updatedAt) {
        UltraPlusStyle style = profile.style();
        SAVE.execute(PLAYER_ID.is(profile.playerId()), GRANTED_DAYS.is(profile.grantedDays()), EMBLEM.is(style.emblem()), PLUS_COLOR.is(style.plusColor()),
            TAG_LEVEL.is(style.tagLevel()), REWARD_MONTH.is(profile.rewardMonth().map(YearMonth::toString)), UPDATED_AT.is(updatedAt));
    }
}
