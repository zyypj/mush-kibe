package br.com.muxe.core.common.customroom.infrastructure.postgres;

import br.com.muxe.core.common.customroom.domain.PresetRepository;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.persistence.sql.Column;
import br.com.muxe.core.common.persistence.sql.Row;
import br.com.muxe.core.common.persistence.sql.Select;
import br.com.muxe.core.common.persistence.sql.Sql;
import br.com.muxe.core.common.persistence.sql.Table;
import br.com.muxe.core.common.persistence.sql.Write;
import br.com.muxe.core.common.persistence.sql.type.SqlType;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PostgresPresetRepository implements PresetRepository {

    private static final Table PRESETS = Table.of("core", "custom_room_presets");
    private static final Column<UUID> ID = PRESETS.column("id", SqlType.UUID);
    private static final Column<String> CODE = PRESETS.column("code", SqlType.TEXT);
    private static final Column<UUID> OWNER = PRESETS.column("owner", SqlType.UUID);
    private static final Column<String> GAME = PRESETS.column("game", SqlType.TEXT);
    private static final Column<String> NAME = PRESETS.column("name", SqlType.TEXT);
    private static final Column<String> MODE = PRESETS.column("mode", SqlType.TEXT);
    private static final Column<String> MAPS = PRESETS.column("maps", SqlType.TEXT);
    private static final Column<String> OPTIONS = PRESETS.column("options", SqlType.TEXT);
    private static final Column<Instant> CREATED_AT = PRESETS.column("created_at", SqlType.TIMESTAMPTZ);

    private static final Select<RoomPreset> OF_OWNER = Sql.select(ID, CODE, OWNER, GAME, NAME, MODE, MAPS, OPTIONS, CREATED_AT).from(PRESETS)
        .where(OWNER.eq().and(GAME.eq()))
        .orderBy(CREATED_AT.asc())
        .map(PostgresPresetRepository::preset)
        .named("custom_room_presets.of_owner");

    private static final Select<RoomPreset> BY_CODE = Sql.select(ID, CODE, OWNER, GAME, NAME, MODE, MAPS, OPTIONS, CREATED_AT).from(PRESETS)
        .where(CODE.eq())
        .map(PostgresPresetRepository::preset)
        .named("custom_room_presets.by_code");

    private static final Select<String> CODE_TAKEN = Sql.select(CODE).from(PRESETS)
        .where(CODE.eq())
        .map(row -> row.get(CODE))
        .named("custom_room_presets.code_taken");

    private static final Write<RoomPreset> SAVE = Sql.<RoomPreset>insertInto(PRESETS)
        .value(ID, RoomPreset::id)
        .value(CODE, RoomPreset::code)
        .value(OWNER, RoomPreset::owner)
        .value(GAME, RoomPreset::game)
        .value(NAME, RoomPreset::name)
        .value(MODE, RoomPreset::mode)
        .value(MAPS, RoomPreset::mapsText)
        .value(OPTIONS, preset -> preset.options().encode())
        .value(CREATED_AT, RoomPreset::createdAt)
        .named("custom_room_presets.save");

    private static final Write<Void> DELETE = Sql.deleteFrom(PRESETS)
        .where(OWNER.eq().and(ID.eq()))
        .named("custom_room_presets.delete");

    @Override
    public List<RoomPreset> ofOwner(UUID owner, String game) {
        return OF_OWNER.list(OWNER.is(owner), GAME.is(game));
    }

    @Override
    public Optional<RoomPreset> byCode(String code) {
        return BY_CODE.first(CODE.is(code));
    }

    @Override
    public boolean codeTaken(String code) {
        return CODE_TAKEN.exists(CODE.is(code));
    }

    @Override
    public void save(RoomPreset preset) {
        SAVE.execute(preset);
    }

    @Override
    public boolean delete(UUID owner, UUID id) {
        return DELETE.execute(OWNER.is(owner), ID.is(id)) > 0;
    }

    private static RoomPreset preset(Row row) throws SQLException {
        return new RoomPreset(row.get(ID), row.get(CODE), row.get(OWNER), row.get(GAME), row.get(NAME), row.get(MODE), RoomPreset.maps(row.get(MAPS)),
            RoomOptions.decode(row.get(OPTIONS)), row.get(CREATED_AT));
    }
}
