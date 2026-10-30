package br.com.muxe.minigame.engine.setup.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.text.ChatButton;
import br.com.muxe.core.bukkit.text.ChatButton.Action;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.setup.SetupChecklist;
import br.com.muxe.minigame.engine.setup.SetupSchema;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.StepKind;
import br.com.muxe.minigame.engine.setup.TeamRule;
import br.com.muxe.minigame.engine.setup.session.SetupMarkers.Marker;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class SetupHelpTest {

    private static final SetupStep WAITING = SetupStep.single("espera", "Spawn da espera", StepKind.LOCATION, false, "&e&lESPERA");
    private static final SetupStep BED = SetupStep.single("cama", "Cama", StepKind.BLOCK, true, "&c&lCAMA", "{team}");
    private static final SetupStep IRON = SetupStep.many("ferro", "Gerador de ferro", StepKind.CENTER, true, 1, 4, "&f&lFERRO #{index}", "{team}");
    private static final SetupStep VOID = SetupStep.single("void", "Altura do void", StepKind.HEIGHT, false);

    private final MessageCatalog messages = SetupMessages.defaults();
    private final SetupHelp help = new SetupHelp(messages);
    private final SetupSchema schema = new SetupSchema() {
        @Override
        public List<String> modes() {
            return List.of("solo", "1v1");
        }

        @Override
        public TeamRule teams(String mode) {
            return mode.equals("solo") ? TeamRule.chosen(8) : TeamRule.fixed(TeamColor.AZUL, TeamColor.VERMELHO);
        }

        @Override
        public List<SetupStep> steps(String mode) {
            return List.of(WAITING, BED, IRON, VOID);
        }
    };

    @Test
    void everyStepIsAClickableLineThatRunsTheRightCommand() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);
        draft.set(BED, TeamColor.AZUL, Point.block(4, 70, 4));
        draft.set(IRON, TeamColor.AZUL, Point.block(0, 70, 8));

        List<List<ChatButton>> lines = help.lines(draft, SetupChecklist.of(schema, draft));

        assertThat(plainLines(lines)).contains(
            "✘ Spawn da espera [Definir]",
            "✔ Cama do Azul [Definir] [-]",
            "✘ Cama do Vermelho [Definir]",
            "✔ Gerador de ferro do Azul (1) [+] [-]",
            "✘ Altura do void [Definir]",
            "[SALVAR]  [NOME]  [SAIR]"
        );
        assertThat(commands(lines)).contains(
            "/arena definir espera",
            "/arena definir cama vermelho",
            "/arena remover cama azul",
            "/arena definir ferro azul",
            "/arena salvar",
            "/arena sair"
        );
        assertThat(lines.getLast().get(1).action()).isEqualTo(Action.SUGGEST);
    }

    @Test
    void chosenTeamsOfferTheMissingColoursAsButtons() {
        ArenaDraft draft = ArenaDraft.create("solo_slip", "solo", schema);
        draft.addTeam(TeamColor.CIANO, schema.teams("solo"));

        List<List<ChatButton>> lines = help.lines(draft, SetupChecklist.of(schema, draft));

        List<ChatButton> teams = lines.stream().filter(line -> plain(line).startsWith("✘ Times")).findFirst().orElseThrow();
        assertThat(plain(teams)).startsWith("✘ Times (1/8): Ciano [+Azul]");
        assertThat(teams).extracting(ChatButton::command).contains("/arena time remover ciano", "/arena time adicionar laranja");
    }

    @Test
    void theActionBarNamesTheNextMissingItemAndHowManyMore() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        String bar = ChatColor.stripColor(help.actionBar(SetupChecklist.of(schema, draft)));

        assertThat(bar).isEqualTo("Falta: Spawn da espera (+5)");
    }

    @Test
    void anArenaWithEverythingSetSaysItIsReady() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);
        draft.set(WAITING, null, Point.block(0, 120, 0));
        draft.set(VOID, null, Point.block(0, 30, 0));
        for (TeamColor team : draft.teams()) {
            draft.set(BED, team, Point.block(0, 70, 0));
            draft.set(IRON, team, Point.block(0, 70, 0));
        }

        assertThat(ChatColor.stripColor(help.actionBar(SetupChecklist.of(schema, draft)))).startsWith("Tudo pronto!");
    }

    @Test
    void eachSetPointGetsAHologramWithItsTeamAndNumber() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);
        draft.set(IRON, TeamColor.VERMELHO, Point.block(1, 70, 1));
        draft.set(IRON, TeamColor.VERMELHO, Point.block(2, 70, 1));
        draft.set(BED, TeamColor.AZUL, Point.block(5, 70, 5));
        draft.set(VOID, null, Point.block(0, 30, 0));

        List<Marker> markers = SetupMarkers.markers(schema, draft, help);

        assertThat(markers).extracting(Marker::lines).containsExactly(
            List.of("&c&lCAMA", "&9Azul"),
            List.of("&f&lFERRO #1", "&cVermelho"),
            List.of("&f&lFERRO #2", "&cVermelho")
        );
        assertThat(markers.getFirst().height()).isEqualTo(SetupMarkers.height(StepKind.BLOCK));
    }

    private static List<String> plainLines(List<List<ChatButton>> lines) {
        return lines.stream().map(SetupHelpTest::plain).toList();
    }

    private static String plain(List<ChatButton> line) {
        StringBuilder text = new StringBuilder();
        line.forEach(button -> text.append(button.label()));
        return ChatColor.stripColor(text.toString().replace('&', '§')).trim();
    }

    private static List<String> commands(List<List<ChatButton>> lines) {
        return lines.stream().flatMap(List::stream).map(ChatButton::command).filter(command -> command != null).toList();
    }
}
