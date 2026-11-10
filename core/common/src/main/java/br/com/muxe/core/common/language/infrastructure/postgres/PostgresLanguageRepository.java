package br.com.muxe.core.common.language.infrastructure.postgres;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageRepository;
import br.com.muxe.core.common.persistence.sql.Column;
import br.com.muxe.core.common.persistence.sql.Select;
import br.com.muxe.core.common.persistence.sql.Sql;
import br.com.muxe.core.common.persistence.sql.Table;
import br.com.muxe.core.common.persistence.sql.Write;
import br.com.muxe.core.common.persistence.sql.type.SqlType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresLanguageRepository implements LanguageRepository {

    private static final Table PLAYER_LANGUAGES = Table.of("core", "player_languages");
    private static final Column<UUID> PLAYER_ID = PLAYER_LANGUAGES.column("player_id", SqlType.UUID);
    private static final Column<Language> LANGUAGE = PLAYER_LANGUAGES.column("language", SqlType.TEXT.map(Language::parse, Language::tag));
    private static final Column<Instant> UPDATED_AT = PLAYER_LANGUAGES.column("updated_at", SqlType.TIMESTAMPTZ);

    private static final Select<Language> FIND = Sql.select(LANGUAGE).from(PLAYER_LANGUAGES)
        .where(PLAYER_ID.eq())
        .map(row -> row.get(LANGUAGE))
        .named("language.find");

    private static final Write<Void> SAVE = Sql.<Void>insertInto(PLAYER_LANGUAGES)
        .value(PLAYER_ID)
        .value(LANGUAGE)
        .value(UPDATED_AT)
        .onConflict(PLAYER_ID).doUpdate(LANGUAGE, UPDATED_AT)
        .named("language.save");

    @Override
    public Optional<Language> find(UUID playerId) {
        return FIND.first(PLAYER_ID.is(playerId));
    }

    @Override
    public void save(UUID playerId, Language language, Instant updatedAt) {
        SAVE.execute(PLAYER_ID.is(playerId), LANGUAGE.is(language), UPDATED_AT.is(updatedAt));
    }
}
