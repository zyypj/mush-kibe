package br.com.muxe.minigame.bedwars.arena;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.mode.GameModeLoader;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.TeamRule;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.io.File;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class BedWarsSchemaTest {

    private final ConfigurationSection modes = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")).getConfigurationSection("modes");
    private final List<GameMode> loaded = GameModeLoader.load(modes);
    private final BedWarsSchema schema = BedWarsSchema.from(loaded, modes);

    @Test
    void theNineModesHaveTheLimitsOfTheSpec() {
        assertThat(loaded).extracting(mode -> mode.id() + " " + mode.minPlayers() + "-" + mode.maxPlayers() + " x" + mode.teamSize()).containsExactly(
            "solo 6-8 x1", "duplas 8-16 x2", "trios 6-12 x3", "quartetos 8-16 x4",
            "1v1 2-2 x1", "2v2 4-4 x2", "3v3 6-6 x3", "4v4 8-8 x4", "4s 8-8 x4"
        );
    }

    @Test
    void soloAndDoublesChooseEightColoursAndTheOthersHaveFixedTeams() {
        assertThat(schema.teams("solo")).isEqualTo(TeamRule.chosen(8));
        assertThat(schema.teams("duplas")).isEqualTo(TeamRule.chosen(8));
        assertThat(schema.teams("trios").fixed()).containsExactly(TeamColor.AZUL, TeamColor.VERMELHO, TeamColor.VERDE, TeamColor.AMARELO);
        assertThat(schema.teams("quartetos").fixed()).hasSize(4);
        for (String mode : List.of("1v1", "2v2", "3v3", "4v4", "4s")) {
            assertThat(schema.teams(mode).fixed()).as(mode).containsExactly(TeamColor.AZUL, TeamColor.VERMELHO);
        }
    }

    @Test
    void everyArenaNeedsTheTeamBasesAndOnlyTheFourIslandModeHasExtras() {
        assertThat(schema.steps("solo")).extracting(SetupStep::id).containsExactly(
            "espera", "area-espera", "espectador", "void", "limite", "diamante", "esmeralda",
            "spawn", "cama", "loja", "melhorias", "ferro", "ouro");
        assertThat(schema.steps("4s")).extracting(SetupStep::id).contains("loja-extra", "melhorias-extra", "ferro-extra", "ouro-extra");
        assertThat(schema.steps("solo")).filteredOn(SetupStep::perTeam).extracting(SetupStep::id).containsExactly("spawn", "cama", "loja", "melhorias", "ferro", "ouro");
    }

    @Test
    void theModeComesFromTheArenaName() {
        assertThat(schema.modeFromName("1v1_slip")).contains("1v1");
        assertThat(schema.modeFromName("4s_ilhas")).contains("4s");
        assertThat(schema.modeFromName("quartetos_air_nation")).contains("quartetos");
        assertThat(schema.displayNameFromName("quartetos_air_nation")).isEqualTo("Air Nation");
    }
}
