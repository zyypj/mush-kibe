package br.com.muxe.core.common.customroom.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.customroom.domain.PresetRepository;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.List;

public final class ListPresetsHandler implements QueryHandler<ListPresetsQuery, List<RoomPreset>> {

    private final PresetRepository presets;
    private final Transactions transactions;

    public ListPresetsHandler(PresetRepository presets, Transactions transactions) {
        this.presets = presets;
        this.transactions = transactions;
    }

    @Override
    public List<RoomPreset> handle(ListPresetsQuery query) {
        return transactions.readOnly("custom_room.presets.list", () -> presets.ofOwner(query.owner(), query.game()));
    }
}
