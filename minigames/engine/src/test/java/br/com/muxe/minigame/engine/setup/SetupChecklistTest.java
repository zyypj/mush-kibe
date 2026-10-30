package br.com.muxe.minigame.engine.setup;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.engine.setup.SetupChecklist.Item;
import br.com.muxe.minigame.engine.team.TeamColor;
import org.junit.jupiter.api.Test;

class SetupChecklistTest {

    private final TestSchema schema = new TestSchema();

    @Test
    void teamStepsAppearOncePerTeamAndOptionalStepsAreNeverMissing() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        SetupChecklist checklist = SetupChecklist.of(schema, draft);

        assertThat(checklist.items()).extracting(item -> item.step().id() + (item.team() == null ? "" : "." + item.team().id()))
            .containsExactly("espera", "area-espera", "void", "spawn.azul", "spawn.vermelho", "ferro.azul", "ferro.vermelho", "diamante");
        assertThat(checklist.missing()).extracting(Item::step).doesNotContain(TestSchema.DIAMOND);
        assertThat(checklist.missingCount()).isEqualTo(7);
        assertThat(checklist.isReady()).isFalse();
    }

    @Test
    void anArenaIsReadyWhenEveryRequiredStepAndTeamIsSet() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);
        draft.set(TestSchema.WAITING, null, Point.block(0, 120, 0));
        draft.set(TestSchema.LOBBY, null, Point.block(-5, 115, -5));
        draft.set(TestSchema.LOBBY, null, Point.block(5, 125, 5));
        draft.set(TestSchema.VOID, null, Point.block(0, 40, 0));
        for (TeamColor team : draft.teams()) {
            draft.set(TestSchema.SPAWN, team, Point.block(0, 70, 0));
            draft.set(TestSchema.IRON, team, Point.block(0, 70, 0));
        }

        assertThat(SetupChecklist.of(schema, draft).isReady()).isTrue();
    }

    @Test
    void chosenTeamsMustReachTheModeCount() {
        ArenaDraft draft = ArenaDraft.create("solo_slip", "solo", schema);
        draft.addTeam(TeamColor.AZUL, schema.teams("solo"));

        SetupChecklist checklist = SetupChecklist.of(schema, draft);

        assertThat(checklist.teamsDone()).isFalse();
        assertThat(checklist.teamCount()).isEqualTo(1);
        assertThat(checklist.teamRule().count()).isEqualTo(8);
    }
}
