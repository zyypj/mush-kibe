package br.com.muxe.core.common.preference.infrastructure.postgres;

import br.com.muxe.core.common.persistence.sql.Column;
import br.com.muxe.core.common.persistence.sql.Select;
import br.com.muxe.core.common.persistence.sql.Sql;
import br.com.muxe.core.common.persistence.sql.Table;
import br.com.muxe.core.common.persistence.sql.Write;
import br.com.muxe.core.common.persistence.sql.type.SqlType;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.preference.domain.PreferenceRepository;
import br.com.muxe.core.common.preference.domain.Preferences;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PostgresPreferenceRepository implements PreferenceRepository {

    private static final Table PLAYER_PREFERENCES = Table.of("core", "player_preferences");
    private static final Column<UUID> PLAYER_ID = PLAYER_PREFERENCES.column("player_id", SqlType.UUID);
    private static final Column<String> PREFERENCE = PLAYER_PREFERENCES.column("preference", SqlType.TEXT);
    private static final Column<String> VALUE = PLAYER_PREFERENCES.column("value", SqlType.TEXT);
    private static final Column<Instant> UPDATED_AT = PLAYER_PREFERENCES.column("updated_at", SqlType.TIMESTAMPTZ);

    private static final Select<Map.Entry<String, String>> FIND = Sql.select(PREFERENCE, VALUE).from(PLAYER_PREFERENCES)
        .where(PLAYER_ID.eq())
        .map(row -> Map.entry(row.get(PREFERENCE), row.get(VALUE)))
        .named("preference.find");

    private static final Write<Void> SAVE = Sql.<Void>insertInto(PLAYER_PREFERENCES)
        .value(PLAYER_ID)
        .value(PREFERENCE)
        .value(VALUE)
        .value(UPDATED_AT)
        .onConflict(PLAYER_ID, PREFERENCE).doUpdate(VALUE, UPDATED_AT)
        .named("preference.save");

    @Override
    public Preferences find(UUID playerId) {
        Map<Preference, String> values = new EnumMap<>(Preference.class);
        for (Map.Entry<String, String> row : FIND.list(PLAYER_ID.is(playerId))) {
            Optional<Preference> preference = Preference.byKey(row.getKey());
            preference.ifPresent(found -> values.put(found, row.getValue()));
        }
        return new Preferences(values);
    }

    @Override
    public void save(UUID playerId, Preference preference, String value, Instant updatedAt) {
        SAVE.execute(PLAYER_ID.is(playerId), PREFERENCE.is(preference.key()), VALUE.is(value), UPDATED_AT.is(updatedAt));
    }
}
