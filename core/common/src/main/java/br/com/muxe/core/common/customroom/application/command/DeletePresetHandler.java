package br.com.muxe.core.common.customroom.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.customroom.domain.PresetRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class DeletePresetHandler implements CommandHandler<DeletePresetCommand, Boolean> {

    private final PresetRepository presets;
    private final Transactions transactions;

    public DeletePresetHandler(PresetRepository presets, Transactions transactions) {
        this.presets = presets;
        this.transactions = transactions;
    }

    @Override
    public Boolean handle(DeletePresetCommand command) {
        return transactions.inTransaction("custom_room.presets.delete", () -> presets.delete(command.owner(), command.preset()));
    }
}
