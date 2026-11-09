package br.com.muxe.core.common.customroom.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.customroom.domain.PresetCodes;
import br.com.muxe.core.common.customroom.domain.PresetRepository;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.util.UUID;

public final class SavePresetHandler implements CommandHandler<SavePresetCommand, Result<RoomPreset>> {

    public static final String BLANK_NAME = "custom_room.preset_blank_name";
    public static final String LONG_NAME = "custom_room.preset_long_name";

    private final PresetRepository presets;
    private final Transactions transactions;
    private final PresetCodes codes;
    private final Clock clock;

    public SavePresetHandler(PresetRepository presets, Transactions transactions, PresetCodes codes, Clock clock) {
        this.presets = presets;
        this.transactions = transactions;
        this.codes = codes;
        this.clock = clock;
    }

    @Override
    public Result<RoomPreset> handle(SavePresetCommand command) {
        String name = command.name().trim();
        if (name.isEmpty()) {
            return Result.failure(BLANK_NAME, "A preset needs a name");
        }
        if (name.length() > RoomPreset.MAX_NAME_LENGTH) {
            return Result.failure(LONG_NAME, "A preset name has at most " + RoomPreset.MAX_NAME_LENGTH + " characters");
        }
        return transactions.inTransaction("custom_room.presets.save", () -> {
            String code = codes.next(presets::codeTaken);
            RoomPreset preset = new RoomPreset(UUID.randomUUID(), code, command.owner(), command.game(), name, command.mode(), command.maps(),
                command.options(), clock.instant());
            presets.save(preset);
            return Result.success(preset);
        });
    }
}
