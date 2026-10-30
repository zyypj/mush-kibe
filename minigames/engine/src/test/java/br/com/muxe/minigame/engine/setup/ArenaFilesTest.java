package br.com.muxe.minigame.engine.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.setup.ArenaFiles.StoredArena;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArenaFilesTest {

    private final TestSchema schema = new TestSchema();

    @TempDir
    Path folder;

    @Test
    void anArenaIsWrittenAsExpandedYamlAndReadBack() throws IOException {
        ArenaFiles files = new ArenaFiles(folder.resolve("arenas"));
        ArenaDraft draft = ArenaDraft.create("solo_slip", "solo", schema);
        draft.addTeam(TeamColor.CIANO, schema.teams("solo"));
        draft.addTeam(TeamColor.ROSA, schema.teams("solo"));
        draft.rename("Slip Clássico");
        draft.set(TestSchema.WAITING, null, new Point(0.5, 120, 0.5, 90, 0));
        draft.set(TestSchema.IRON, TeamColor.ROSA, Point.block(10, 70, -3));
        draft.set(TestSchema.IRON, TeamColor.ROSA, Point.block(11, 70, -3));
        draft.set(TestSchema.VOID, null, Point.block(0, 38, 0));

        files.write(draft, false);
        StoredArena read = files.read("solo_slip").orElseThrow();

        String yaml = Files.readString(files.file("solo_slip"));
        assertThat(yaml).doesNotContain("{").doesNotContain("[");
        assertThat(read.ready()).isFalse();
        assertThat(read.draft().mode()).isEqualTo("solo");
        assertThat(read.draft().displayName()).isEqualTo("Slip Clássico");
        assertThat(read.draft().teams()).containsExactly(TeamColor.CIANO, TeamColor.ROSA);
        assertThat(read.draft().points("espera")).containsExactly(new Point(0.5, 120, 0.5, 90, 0));
        assertThat(read.draft().points("ferro.rosa")).containsExactly(Point.block(10, 70, -3), Point.block(11, 70, -3));
        assertThat(read.draft().number("void")).hasValue(38);
        assertThat(files.names()).containsExactly("solo_slip");
    }

    @Test
    void aMissingArenaIsEmptyAndABrokenOneFailsClearly() throws IOException {
        ArenaFiles files = new ArenaFiles(folder);
        Files.writeString(files.file("solo_x"), "teams: [");

        assertThat(files.read("solo_y")).isEmpty();
        assertThatThrownBy(() -> files.read("solo_x")).isInstanceOf(IOException.class).hasMessageContaining("solo_x");
    }
}
