package br.com.muxe.minigame.engine.setup;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;

final class TestSchema implements SetupSchema {

    static final SetupStep WAITING = SetupStep.single("espera", "Spawn da espera", StepKind.LOCATION, false, "&e&lESPERA");
    static final SetupStep LOBBY = new SetupStep("area-espera", "Área da espera", StepKind.REGION, false, 2, 2, List.of("&e&lÁREA {index}"));
    static final SetupStep VOID = SetupStep.single("void", "Altura do void", StepKind.HEIGHT, false);
    static final SetupStep SPAWN = SetupStep.single("spawn", "Spawn", StepKind.LOCATION, true, "&a&lSPAWN", "{team}");
    static final SetupStep IRON = SetupStep.many("ferro", "Gerador de ferro", StepKind.CENTER, true, 1, 3, "&f&lFERRO #{index}", "{team}");
    static final SetupStep DIAMOND = SetupStep.many("diamante", "Gerador de diamante", StepKind.CENTER, false, 0, 8, "&b&lDIAMANTE #{index}");

    @Override
    public List<String> modes() {
        return List.of("solo", "1v1");
    }

    @Override
    public TeamRule teams(String mode) {
        return mode.equals("1v1") ? TeamRule.fixed(TeamColor.AZUL, TeamColor.VERMELHO) : TeamRule.chosen(8);
    }

    @Override
    public List<SetupStep> steps(String mode) {
        return List.of(WAITING, LOBBY, VOID, SPAWN, IRON, DIAMOND);
    }
}
