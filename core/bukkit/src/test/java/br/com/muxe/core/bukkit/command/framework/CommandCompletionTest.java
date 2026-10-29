package br.com.muxe.core.bukkit.command.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandCompletionTest {

    private TestCommands commands;

    @BeforeEach
    void register() {
        commands = TestCommands.create();
        commands.commands().suggestions("friends", sender -> List.of("Ana", "Bia"));
        commands.register(new SampleCommands.Warp(), new SampleCommands.Panel(), new SampleCommands.Pair());
    }

    @Test
    void subcommandsAreFilteredByPermissionAndSender() {
        FakeSender player = FakeSender.named("Steve");
        FakeSender staff = FakeSender.named("Staff", SampleCommands.ANNOUNCE);
        FakeSender console = FakeSender.named("CONSOLE", SampleCommands.ANNOUNCE);

        assertThat(commands.complete(player.player(), "painel ")).containsExactly("erro", "jogador", "modo", "nota");
        assertThat(commands.complete(staff.player(), "painel ")).containsExactly("anunciar", "erro", "jogador", "modo", "nota");
        assertThat(commands.complete(console.console(), "painel ")).containsExactly("anunciar", "erro", "jogador", "modo", "nota", "parar");
    }

    @Test
    void aCommandWithoutPermissionSuggestsNothing() {
        FakeSender player = FakeSender.named("Steve");

        assertThat(commands.complete(player.player(), "warp ")).isEmpty();
        assertThat(commands.complete(player.player(), "warp ir ")).isEmpty();
    }

    @Test
    void wordsAreFilteredByWhatWasTypedIgnoringCase() {
        FakeSender player = FakeSender.named("Steve");

        assertThat(commands.complete(player.player(), "painel MO")).containsExactly("modo");
        assertThat(commands.complete(player.player(), "painel modo ")).containsExactly("definir", "limite");
        assertThat(commands.complete(player.player(), "painel modo definir c")).containsExactly("criativo");
    }

    @Test
    void aliasesAppearOnlyOnceSomethingIsTyped() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        assertThat(commands.complete(staff.player(), "warp ")).containsExactly("criar", "ir");
        assertThat(commands.complete(staff.player(), "w c")).containsExactly("create", "criar");
    }

    @Test
    void argumentsSuggestFromTheirType() {
        FakeSender player = FakeSender.named("Steve");

        assertThat(commands.complete(player.player(), "painel modo definir ")).containsExactly("criativo", "sobrevivencia-extrema");
        assertThat(commands.complete(player.player(), "painel modo definir criativo ")).containsExactly("não", "sim");
        assertThat(commands.complete(player.player(), "painel modo limite ")).isEmpty();
        assertThat(commands.complete(player.player(), "painel modo definir criativo sim ")).isEmpty();
    }

    @Test
    void fixedValuesAndProvidersAreSuggested() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        assertThat(commands.complete(staff.player(), "warp ir ")).containsExactly("loja", "norte", "spawn", "sul");
        assertThat(commands.complete(staff.player(), "warp ir s")).containsExactly("spawn", "sul");
        assertThat(commands.complete(staff.player(), "painel jogador ")).containsExactly("Ana", "Bia");
    }

    @Test
    void textArgumentsSuggestNothing() {
        FakeSender player = FakeSender.named("Steve");

        assertThat(commands.complete(player.player(), "entrar ")).isEmpty();
        assertThat(commands.complete(player.player(), "painel nota ")).isEmpty();
    }
}
