package br.com.muxe.minigame.engine.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.setup.ArenaDraft.Change;
import br.com.muxe.minigame.engine.team.TeamColor;
import org.junit.jupiter.api.Test;

class ArenaDraftTest {

    private final TestSchema schema = new TestSchema();

    @Test
    void theNameGivesTheModeAndTheMapName() {
        assertThat(schema.modeFromName("1v1_slip")).contains("1v1");
        assertThat(schema.modeFromName("duplas_slip")).isEmpty();
        assertThat(schema.modeFromName("slip")).isEmpty();
        assertThat(schema.displayNameFromName("1v1_air_nation")).isEqualTo("Air Nation");
        assertThat(schema.displayNameFromName("slip")).isEqualTo("Slip");
    }

    @Test
    void aFixedTeamModeStartsWithItsTeams() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        assertThat(draft.teams()).containsExactly(TeamColor.AZUL, TeamColor.VERMELHO);
        assertThat(draft.displayName()).isEqualTo("Slip");
        assertThat(draft.addTeam(TeamColor.VERDE, schema.teams("1v1"))).isFalse();
    }

    @Test
    void chosenTeamsStopAtTheModeCountAndTakeTheirPointsWhenRemoved() {
        ArenaDraft draft = ArenaDraft.create("solo_slip", "solo", schema);
        TeamRule rule = schema.teams("solo");
        for (TeamColor color : TeamColor.values()) {
            draft.addTeam(color, rule);
        }
        draft.set(TestSchema.SPAWN, TeamColor.AZUL, Point.block(1, 70, 1));

        assertThat(draft.teams()).hasSize(8).doesNotContain(TeamColor.LARANJA);
        assertThat(draft.removeTeam(TeamColor.AZUL, rule)).isTrue();
        assertThat(draft.points("spawn.azul")).isEmpty();
    }

    @Test
    void aSinglePointIsReplacedAndAListGrowsUntilItsMaximum() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        assertThat(draft.set(TestSchema.WAITING, null, Point.block(0, 120, 0)).type()).isEqualTo(Change.Type.SET);
        assertThat(draft.set(TestSchema.WAITING, null, Point.block(0, 121, 0)).type()).isEqualTo(Change.Type.REPLACED);
        assertThat(draft.points(TestSchema.WAITING, null)).containsExactly(Point.block(0, 121, 0));
        for (int index = 1; index <= 3; index++) {
            assertThat(draft.set(TestSchema.IRON, TeamColor.AZUL, Point.block(index, 70, 0))).isEqualTo(new Change(Change.Type.ADDED, index, index));
        }
        assertThat(draft.set(TestSchema.IRON, TeamColor.AZUL, Point.block(9, 70, 0)).type()).isEqualTo(Change.Type.FULL);
    }

    @Test
    void aRegionTakesTwoCornersAndStartsOverOnTheThird() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        draft.set(TestSchema.LOBBY, null, Point.block(-10, 110, -10));
        draft.set(TestSchema.LOBBY, null, Point.block(10, 130, 10));
        draft.set(TestSchema.LOBBY, null, Point.block(5, 115, 5));

        assertThat(draft.points(TestSchema.LOBBY, null)).containsExactly(Point.block(5, 115, 5));
    }

    @Test
    void theHeightStepKeepsTheBlockYOfThePlayer() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        draft.set(TestSchema.VOID, null, new Point(3.2, 41.9, 7, 0, 0));

        assertThat(draft.number("void")).hasValue(41);
        assertThat(draft.count(TestSchema.VOID, null)).isEqualTo(1);
        assertThat(draft.remove(TestSchema.VOID, null, null)).isTrue();
        assertThat(draft.number("void")).isEmpty();
    }

    @Test
    void removingFromAListTakesTheNearestPoint() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);
        draft.set(TestSchema.DIAMOND, null, Point.block(30, 70, 0));
        draft.set(TestSchema.DIAMOND, null, Point.block(-30, 70, 0));

        draft.remove(TestSchema.DIAMOND, null, Point.block(-28, 70, 2));

        assertThat(draft.points(TestSchema.DIAMOND, null)).containsExactly(Point.block(30, 70, 0));
    }

    @Test
    void aTeamStepNeedsATeamAndAnArenaStepRefusesOne() {
        ArenaDraft draft = ArenaDraft.create("1v1_slip", "1v1", schema);

        assertThatThrownBy(() -> draft.set(TestSchema.SPAWN, null, Point.block(0, 0, 0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> draft.set(TestSchema.WAITING, TeamColor.AZUL, Point.block(0, 0, 0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void namesAreLowercaseWithUnderscores() {
        assertThat(ArenaDraft.isValidName("1v1_slip")).isTrue();
        assertThat(ArenaDraft.isValidName("1v1 slip")).isFalse();
        assertThat(ArenaDraft.isValidName("../x")).isFalse();
    }

    @Test
    void yawSnapsToTheNearestEighthOfATurn() {
        assertThat(new Point(1.7, 70.4, -3.2, -93, 30).centered()).isEqualTo(new Point(1.5, 70, -3.5, 270, 0));
        assertThat(Point.snap(359)).isZero();
    }
}
