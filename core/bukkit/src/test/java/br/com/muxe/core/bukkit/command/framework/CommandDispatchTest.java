package br.com.muxe.core.bukkit.command.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandDispatchTest {

    private final SampleCommands.Warp warp = new SampleCommands.Warp();
    private final SampleCommands.Panel panel = new SampleCommands.Panel();
    private final SampleCommands.Home home = new SampleCommands.Home();
    private final SampleCommands.Help help = new SampleCommands.Help();
    private final SampleCommands.Pair pair = new SampleCommands.Pair();
    private TestCommands commands;

    @BeforeEach
    void register() {
        commands = TestCommands.create();
        commands.commands().suggestions("friends", sender -> List.of("Ana", "Bia"));
        commands.register(warp, panel, home, help, pair);
    }

    @Test
    void theDefaultHandlerRunsWhenNoSubcommandIsGiven() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        commands.run(staff.player(), "warp");

        assertThat(warp.calls).containsExactly("list");
        assertThat(staff.messages()).isEmpty();
    }

    @Test
    void theCommandPermissionIsCheckedBeforeAnything() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.player(), "warp");
        commands.run(player.player(), "warp criar casa");

        assertThat(warp.calls).isEmpty();
        assertThat(player.messages()).containsExactly("no-permission", "no-permission");
    }

    @Test
    void aSubcommandPermissionAddsToTheCommandOne() {
        FakeSender player = FakeSender.named("Steve");
        FakeSender staff = FakeSender.named("Staff", SampleCommands.ANNOUNCE);

        commands.run(player.sender(), "painel anunciar oi");
        commands.run(staff.sender(), "painel anunciar oi");

        assertThat(player.messages()).containsExactly("no-permission");
        assertThat(panel.calls).containsExactly("announce oi");
    }

    @Test
    void theSenderTypeComesFromTheFirstParameter() {
        FakeSender console = FakeSender.named("CONSOLE", SampleCommands.WARP);
        FakeSender player = FakeSender.named("Steve", SampleCommands.WARP);

        commands.run(console.console(), "warp criar casa");
        commands.run(player.player(), "painel parar");
        commands.run(console.console(), "painel parar");
        commands.run(console.console(), "warp");

        assertThat(console.messages()).containsExactly("players-only");
        assertThat(player.messages()).containsExactly("console-only");
        assertThat(warp.calls).containsExactly("list");
        assertThat(panel.calls).containsExactly("stop");
    }

    @Test
    void labelsAndSubcommandsMatchTheirAliasesIgnoringCase() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        commands.run(staff.player(), "w CREATE casa");
        commands.run(staff.player(), "warp Criar loja");

        assertThat(warp.calls).containsExactly("create casa 10", "create loja 10");
    }

    @Test
    void optionalArgumentsTakeTheirDefaultOrTheGivenValue() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        commands.run(staff.player(), "warp criar casa");
        commands.run(staff.player(), "warp criar casa 5");

        assertThat(warp.calls).containsExactly("create casa 10", "create casa 5");
    }

    @Test
    void anInvalidNumberIsReportedWithTheTypedValue() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        commands.run(staff.player(), "warp criar casa dez");
        commands.run(staff.sender(), "painel modo limite 5 x");

        assertThat(warp.calls).isEmpty();
        assertThat(panel.calls).isEmpty();
        assertThat(staff.messages()).containsExactly("invalid number dez", "invalid number x");
    }

    @Test
    void missingOrExtraArgumentsShowTheGeneratedUsageOfThatSubcommand() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.WARP);

        commands.run(staff.player(), "warp criar");
        commands.run(staff.player(), "w criar casa 5 extra");

        assertThat(warp.calls).isEmpty();
        assertThat(staff.messages()).containsExactly("usage /warp criar <nome> [custo]", "usage /w criar <nome> [custo]");
    }

    @Test
    void aGroupWithoutDefaultListsWhatTheSenderMayUse() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.player(), "painel");

        assertThat(player.messages()).containsExactly(
            "usage of /painel",
            "- /painel erro",
            "- /painel jogador <name>",
            "- /painel modo definir <mode> <on>",
            "- /painel modo limite <max> <ratio>",
            "- /painel nota [texto...]"
        );
    }

    @Test
    void anUnknownSubcommandFallsBackToTheUsage() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.sender(), "painel modo");

        assertThat(player.messages()).containsExactly(
            "usage of /painel",
            "- /painel modo definir <mode> <on>",
            "- /painel modo limite <max> <ratio>"
        );
    }

    @Test
    void theConfiguredUsageMessageWinsOverTheGeneratedOne() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.player(), "casa");
        commands.run(player.player(), "casa ir");
        commands.run(player.player(), "entrar");

        assertThat(home.calls).isEmpty();
        assertThat(player.messages()).containsExactly("custom usage", "second line", "custom usage", "second line", "custom usage", "second line");
    }

    @Test
    void aGroupThatOnlyPlayersUseRefusesTheConsoleBeforeTheUsage() {
        FakeSender console = FakeSender.named("CONSOLE");

        commands.run(console.console(), "casa");

        assertThat(console.messages()).containsExactly("players-only");
    }

    @Test
    void aUsageMethodRepliesInsteadOfTheMessages() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.sender(), "ajuda");
        commands.run(player.sender(), "ajuda ver");
        commands.run(player.sender(), "ajuda ver 2");

        assertThat(help.calls).containsExactly("usage", "usage", "show 2");
        assertThat(player.messages()).isEmpty();
    }

    @Test
    void nestedPathsResolveEnumsBooleansAndDecimals() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.sender(), "painel modo definir criativo sim");
        commands.run(player.sender(), "painel modo definir SOBREVIVENCIA-EXTREMA off");
        commands.run(player.sender(), "painel modo limite 5 1,5");

        assertThat(panel.calls).containsExactly("mode CRIATIVO true", "mode SOBREVIVENCIA_EXTREMA false", "limit 5 1.5");
    }

    @Test
    void invalidOptionsListTheAcceptedWords() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.sender(), "painel modo definir aventura sim");
        commands.run(player.sender(), "painel modo definir criativo talvez");

        assertThat(panel.calls).isEmpty();
        assertThat(player.messages()).containsExactly(
            "invalid option aventura: criativo, sobrevivencia-extrema",
            "invalid option talvez: sim, não"
        );
    }

    @Test
    void aGreedyArgumentTakesTheRestOfTheLine() {
        FakeSender staff = FakeSender.named("Staff", SampleCommands.ANNOUNCE);

        commands.run(staff.sender(), "painel anunciar &b&lRANK ENDERLORE");
        commands.run(staff.sender(), "painel nota");
        commands.run(staff.sender(), "painel nota uma nota longa");
        commands.run(staff.sender(), "painel anunciar");

        assertThat(panel.calls).containsExactly("announce &b&lRANK ENDERLORE", "note -", "note uma nota longa");
        assertThat(staff.messages()).containsExactly("usage /painel anunciar <text...>");
    }

    @Test
    void methodLevelCommandsBecomeSeparateCommands() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.player(), "login segredo");
        commands.run(player.player(), "sair");
        commands.run(player.player(), "entrar a b");

        assertThat(pair.calls).containsExactly("login segredo", "logout");
        assertThat(player.messages()).containsExactly("custom usage", "second line");
    }

    @Test
    void aFailingHandlerIsReportedToTheSender() {
        FakeSender player = FakeSender.named("Steve");

        commands.run(player.sender(), "painel erro");

        assertThat(player.messages()).containsExactly("failed");
    }
}
