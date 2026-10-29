package br.com.muxe.core.bukkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.command.framework.TestCommands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.placement.PlacementImport;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.Placements;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class NpcImportCommandTest {

    private static final MessageCatalog MESSAGES = MessageCatalog.from(ShopFixtures.CONFIG.getConfigurationSection("messages"));

    @Test
    void staffImportTheFileRightAwayAndSeeHowManyCameIn() {
        FakeSender staff = run(() -> CompletableFuture.completedFuture(new PlacementImport.Outcome(true, 12)));

        assertThat(staff.messages()).containsExactly(
            MESSAGES.get("npc-importing", "file", "import.yml"),
            MESSAGES.get("npc-imported", "count", "12", "file", "import.yml", "group", "lobby")
        );
    }

    @Test
    void aMissingFileIsReported() {
        FakeSender staff = run(() -> CompletableFuture.completedFuture(PlacementImport.Outcome.NO_FILE));

        assertThat(staff.messages()).containsExactly(
            MESSAGES.get("npc-importing", "file", "import.yml"),
            MESSAGES.get("npc-import-missing", "file", "import.yml")
        );
    }

    @Test
    void aFailedSaveShowsThePlacementFailure() {
        FakeSender staff = run(() -> CompletableFuture.failedFuture(new IllegalStateException("database down")));

        assertThat(staff.messages()).containsExactly(
            MESSAGES.get("npc-importing", "file", "import.yml"),
            MESSAGES.get(AsyncReply.FAILED)
        );
    }

    private static FakeSender run(Supplier<CompletableFuture<PlacementImport.Outcome>> importer) {
        TestCommands commands = new TestCommands(ArgumentTypes.basic(), MESSAGES);
        PlacementArgument.registerAll(commands.commands(), () -> Placements.NONE);
        PlacementSync placements = new PlacementSync("lobby", null, null, null, Runnable::run, Logger.getLogger("test"));
        commands.register(new NpcCommand(placements, null, importer, Runnable::run, MESSAGES, null));
        FakeSender staff = FakeSender.named("Staff", NpcCommand.PERMISSION);
        commands.run(staff.player(), "npc importar");
        return staff;
    }
}
