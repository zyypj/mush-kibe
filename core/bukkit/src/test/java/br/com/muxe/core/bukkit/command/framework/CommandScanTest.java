package br.com.muxe.core.bukkit.command.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.command.framework.argument.ArgumentResult;
import br.com.muxe.core.bukkit.command.framework.node.CommandDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandScanTest {

    @Test
    void theAnnotationsDescribeTheCommand() {
        TestCommands commands = TestCommands.create().register(new SampleCommands.Warp(), new SampleCommands.Pair());

        CommandDefinition warp = commands.command("warp").definition();
        CommandDefinition login = commands.command("entrar").definition();

        assertThat(warp.aliases()).containsExactly("w");
        assertThat(warp.description()).isEqualTo("Warps.");
        assertThat(warp.permission()).contains(SampleCommands.WARP);
        assertThat(warp.root().children()).extracting(node -> node.name()).containsExactly("criar", "ir");
        assertThat(login.aliases()).containsExactly("login");
        assertThat(login.usageKey()).contains("custom-usage");
        assertThat(commands.command("sair").definition().usageKey()).isEmpty();
    }

    @Test
    void aClassWithoutCommandsIsRejected() {
        TestCommands commands = TestCommands.create();

        assertThatThrownBy(() -> commands.register(new SampleCommands.Unannotated())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidDeclarationsFailWhenRegistering() {
        for (Object invalid : List.of(
            new SampleCommands.RequiredAfterOptional(),
            new SampleCommands.GreedyFirst(),
            new SampleCommands.UnknownType(),
            new SampleCommands.UnknownProvider(),
            new SampleCommands.Duplicated()
        )) {
            TestCommands commands = TestCommands.create();

            assertThatThrownBy(() -> commands.register(invalid))
                .as(invalid.getClass().getSimpleName())
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void customArgumentTypesAreUsedForTheirParameters() {
        TestCommands commands = TestCommands.create();
        commands.commands().argument(StringBuilder.class, (sender, input) -> input.equals("x")
            ? ArgumentResult.rejected("custom-usage")
            : ArgumentResult.of(new StringBuilder(input)));
        commands.register(new SampleCommands.UnknownType());
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.sender(), "ruim abc");
        commands.run(player.sender(), "ruim x");

        assertThat(player.messages()).containsExactly("custom usage", "second line");
    }
}
