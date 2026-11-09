package br.com.muxe.core.common.customroom;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.customroom.application.command.DeletePresetCommand;
import br.com.muxe.core.common.customroom.application.command.DeletePresetHandler;
import br.com.muxe.core.common.customroom.application.command.SavePresetCommand;
import br.com.muxe.core.common.customroom.application.command.SavePresetHandler;
import br.com.muxe.core.common.customroom.application.query.FindPresetHandler;
import br.com.muxe.core.common.customroom.application.query.FindPresetQuery;
import br.com.muxe.core.common.customroom.application.query.ListPresetsHandler;
import br.com.muxe.core.common.customroom.application.query.ListPresetsQuery;
import br.com.muxe.core.common.customroom.domain.PresetCodes;
import br.com.muxe.core.common.customroom.domain.PresetRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.security.SecureRandom;
import java.time.Clock;

public final class CustomRoomModule {

    private CustomRoomModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, PresetRepository presets, Transactions transactions, Clock clock) {
        commands.register(SavePresetCommand.class, new SavePresetHandler(presets, transactions, new PresetCodes(new SecureRandom()), clock));
        commands.register(DeletePresetCommand.class, new DeletePresetHandler(presets, transactions));
        queries.register(ListPresetsQuery.class, new ListPresetsHandler(presets, transactions));
        queries.register(FindPresetQuery.class, new FindPresetHandler(presets, transactions));
    }
}
