package br.com.muxe.core.common.customroom.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.customroom.domain.PresetCodes;
import br.com.muxe.core.common.customroom.domain.PresetRepository;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.Optional;

public final class FindPresetHandler implements QueryHandler<FindPresetQuery, Optional<RoomPreset>> {

    private final PresetRepository presets;
    private final Transactions transactions;

    public FindPresetHandler(PresetRepository presets, Transactions transactions) {
        this.presets = presets;
        this.transactions = transactions;
    }

    @Override
    public Optional<RoomPreset> handle(FindPresetQuery query) {
        return transactions.readOnly("custom_room.presets.find", () -> presets.byCode(PresetCodes.normalize(query.code())));
    }
}
