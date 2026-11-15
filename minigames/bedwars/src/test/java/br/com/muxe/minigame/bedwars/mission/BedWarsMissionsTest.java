package br.com.muxe.minigame.bedwars.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.mission.announce.MissionTexts;
import br.com.muxe.core.bukkit.mission.config.MissionConfig;
import br.com.muxe.core.bukkit.mission.menu.MissionView;
import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.core.bukkit.npc.NpcLabelLayout;
import br.com.muxe.core.common.mission.domain.Mission;
import br.com.muxe.core.common.mission.domain.MissionBoard;
import br.com.muxe.core.common.mission.domain.MissionCadence;
import br.com.muxe.core.common.mission.domain.MissionSession;
import br.com.muxe.minigame.bedwars.lobby.settings.StatsNpcSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopTexts;
import br.com.muxe.minigame.bedwars.lobby.top.TopView;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BedWarsMissionsTest {

    private static final MissionConfig CONFIG = MissionConfig.from(RoomFixtures.CONFIG.getConfigurationSection("missions"), BedWarsGoal::find);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOW = Instant.parse("2026-11-04T15:00:00Z");

    @Test
    void theCatalogHasTheExportedMissionsWithoutHalloween() {
        assertThat(CONFIG.catalog().daily()).extracting(Mission::id).containsExactly("ninguem-me-para", "quem-foi", "protecao-do-fim", "rei-das-pontes",
            "um-aliado-inesperado", "colecionador-de-esmeraldas", "que-preguica", "fim-da-linha-diaria", "pra-que-mais");
        assertThat(CONFIG.catalog().weekly()).extracting(Mission::id).containsExactly("sou-eu-bola-de-fogo", "fim-da-linha", "olha-a-explosao",
            "ansiedade-mais-de-8000", "artesao", "colecionador-de-diamantes", "arqueiro", "afiado", "lutador-eficiente", "milkshake");
        assertThat(CONFIG.catalog().creditPath()).extracting(Mission::credits).containsExactly(15L);
        assertThat(CONFIG.catalog().dailyCount()).isEqualTo(3);
        assertThat(CONFIG.texts("fim-da-linha").name()).isEqualTo("Fim da linha");
        assertThat(MissionRules.from(RoomFixtures.CONFIG.getConfigurationSection("missions"))).isEqualTo(MissionRules.DEFAULT);
    }

    @Test
    void theExportedMissionsHaveTheirGoalsTargetsAndRewards() {
        assertThat(mission(CONFIG.catalog().daily(), "que-preguica")).isEqualTo(new Mission("que-preguica", MissionCadence.DAILY,
            BedWarsGoal.BRIDGE_EGGS.goal(), 2, 625, 0));
        assertThat(mission(CONFIG.catalog().daily(), "fim-da-linha-diaria")).isEqualTo(new Mission("fim-da-linha-diaria", MissionCadence.DAILY,
            BedWarsGoal.FINAL_KILLS.goal(), 5, 625, 0));
        assertThat(mission(CONFIG.catalog().daily(), "pra-que-mais")).isEqualTo(new Mission("pra-que-mais", MissionCadence.DAILY,
            BedWarsGoal.FEW_BLOCKS_WIN.goal(), 1, 625, 0));
        assertThat(CONFIG.catalog().weekly().subList(5, 10)).containsExactly(
            new Mission("colecionador-de-diamantes", MissionCadence.WEEKLY, BedWarsGoal.DIAMONDS.goal(), 150, 2500, 15),
            new Mission("arqueiro", MissionCadence.WEEKLY, BedWarsGoal.BOW_DAMAGE.goal(), 500, 2500, 15),
            new Mission("afiado", MissionCadence.WEEKLY, BedWarsGoal.SHARPNESS_TWO.goal(), 1, 2500, 15),
            new Mission("lutador-eficiente", MissionCadence.WEEKLY, BedWarsGoal.KILLS.goal(), 100, 2500, 15),
            new Mission("milkshake", MissionCadence.WEEKLY, BedWarsGoal.MILK_CLEANSE.goal(), 3, 2500, 15));
        assertThat(CONFIG.texts("fim-da-linha-diaria").name()).isEqualTo("Fim da linha");
    }

    @Test
    void theMenuShowsTheNewMissionsAsExported() {
        MissionConfig.Menu menu = CONFIG.menu();
        MissionBoard board = board(List.of(
            mission(CONFIG.catalog().daily(), "que-preguica"),
            mission(CONFIG.catalog().weekly(), "afiado"),
            mission(CONFIG.catalog().weekly(), "lutador-eficiente")
        ), Map.of("KILLS", 100L));
        MissionBoard.Entry lazy = board.entries().get(0);
        MissionBoard.Entry sharp = board.entries().get(1);
        MissionBoard.Entry fighter = board.entries().get(2);

        assertThat(MissionView.name(menu, CONFIG.texts("que-preguica"), lazy)).isEqualTo("§eQue preguiça...");
        assertThat(MissionView.missionLore(menu, CONFIG.texts("que-preguica"), lazy, "Diária")).containsExactly(
            "&7Diária", " ", "&fUtilize 2 Ovos das Pontes:", "&f&a0/2", " ", "&eRecompensa:", "&b+625 XP (Bed Wars)", " ", "&eEm progresso.");
        assertThat(MissionView.missionLore(menu, CONFIG.texts("afiado"), sharp, "Semanal")).containsExactly(
            "&7Semanal", " ", "&fMelhore sua espada para", "&fAfiação II em 1 partida:", "&f&a0/1", " ", "&eRecompensas:", "&b+2.500 XP (Bed Wars)",
            "&d+15 créditos", " ", "&eEm progresso.");
        assertThat(MissionView.name(menu, CONFIG.texts("lutador-eficiente"), fighter)).isEqualTo("§aLutador eficiente");
        assertThat(MissionView.missionLore(menu, CONFIG.texts("lutador-eficiente"), fighter, "Semanal")).containsExactly(
            "&7Semanal", " ", "&fElimine 100 adversários:", "&f&a100/100", " ", "&eRecompensas:", "&b+2.500 XP (Bed Wars)", "&d+15 créditos", " ",
            "&aCompleta!");
        assertThat(MissionView.rows(menu, false)).isEqualTo(5);
        assertThat(MissionView.rows(menu, true)).isEqualTo(6);
    }

    @Test
    void completingAMissionShowsTheReferenceLineHoverAndReward() {
        MissionSession session = session();
        List<Mission> completed = session.update(Map.of("KILL_STREAK", 2L));
        MissionBoard.Entry entry = entry(session, "ninguem-me-para");

        MissionTexts.Completion completion = MissionTexts.completion(CONFIG, entry);

        assertThat(completed).extracting(Mission::id).containsExactly("ninguem-me-para");
        assertThat(completion.prefix()).isEqualTo("&b&lMISSÕES &aVocê completou ");
        assertThat(completion.name()).isEqualTo("&eNinguém me para! &7(Diária)");
        assertThat(completion.hover()).containsExactly("&eNinguém me para! &7(Diária)", "", "&7Elimine 2 adversários seguidos em uma partida: &f2/2");
        assertThat(completion.rewards()).containsExactly("&b+625 XP (Bed Wars)");
    }

    @Test
    void theMatchSummaryListsWhatMovedPerCategory() {
        MissionSession session = session();
        session.update(Map.of("KILL_STREAK", 2L, "FINAL_KILLS", 3L, "FAST_WIN", 1L));

        List<String> lines = MissionTexts.summary(CONFIG, session.changes());

        assertThat(lines).containsExactly(
            "&a----------------------------------------",
            "&aMISSÕES DIÁRIAS",
            "",
            " &eNinguém me para!: &72/2 &a(+2) &bCOMPLETO!",
            "",
            "&6MISSÕES SEMANAIS",
            "",
            " &eFim da linha: &73/35 &a(+3)",
            " &eAnsiedade mais de 8000: &71/1 &a(+1) &bCOMPLETO!",
            "",
            "&dCAMINHO DOS CRÉDITOS",
            "",
            " &eComplecionista: &71/5 &a(+1)",
            "",
            "&a----------------------------------------"
        );
    }

    @Test
    void theStatsNpcLinesSitAtTheReferenceHeights() {
        NpcLabelLayout labels = StatsNpcSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.stats-npc")).labels();
        List<String> lines = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i");

        List<Double> heights = labels.place(lines).stream().map(DisplayLabel::height).toList();

        double bottom = heights.getLast();
        assertThat(heights.stream().map(height -> Math.round((height - bottom) * 1e6) / 1e6).toList())
            .containsExactly(2.75, 2.46875, 2.03125, 1.75, 1.46875, 1.1875, 0.75, 0.28125, 0.0);
    }

    @Test
    void theModeNpcLinesSitAtTheReferenceHeights() {
        NpcLabelLayout labels = NpcLabelLayout.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.npc-labels"));

        List<Double> heights = labels.place(List.of("&6&lUTILIZE", "&a/duel <nome>", "&b&l1V1", "&e69 jogando")).stream().map(DisplayLabel::height).toList();

        assertThat(heights.get(2) - heights.get(3)).isCloseTo(0.28125, within(1e-9));
        assertThat(heights.get(1) - heights.get(2)).isCloseTo(0.46875, within(1e-9));
        assertThat(heights.get(0) - heights.get(1)).isCloseTo(0.28125, within(1e-9));
    }

    @Test
    void theTopLinesSitAtTheReferenceHeights() {
        TopTexts texts = TopTexts.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.tops"));
        List<Double> gaps = TopView.gaps(texts.spacing());
        double[] depth = new double[gaps.size() + 1];
        for (int index = 0; index < gaps.size(); index++) {
            depth[index + 1] = depth[index] + gaps.get(index);
        }

        assertThat(texts.labelAboveStand()).isEqualTo(PacketHologramTransport.FULL_STAND_LABEL_HEIGHT);
        assertThat(depth[1]).isEqualTo(79.78125 - 79.5);
        assertThat(depth[2]).isEqualTo(79.78125 - 79.125);
        assertThat(depth[11]).isEqualTo(79.78125 - 76.59375);
        assertThat(depth[12]).isEqualTo(79.78125 - 76.21875);
        assertThat(depth[13]).isEqualTo(79.78125 - 75.875);
    }

    private static MissionSession session() {
        List<Mission> active = List.of(
            mission(CONFIG.catalog().daily(), "ninguem-me-para"),
            mission(CONFIG.catalog().weekly(), "fim-da-linha"),
            mission(CONFIG.catalog().weekly(), "ansiedade-mais-de-8000"),
            CONFIG.catalog().creditPath().getFirst()
        );
        return new MissionSession(MissionBoard.of(active, Map.of(), MissionBoard.starts(active, NOW, ZONE)));
    }

    private static MissionBoard board(List<Mission> active, Map<String, Long> amounts) {
        return MissionBoard.of(active, Map.of(), MissionBoard.starts(active, NOW, ZONE)).apply(amounts).board();
    }

    private static Mission mission(List<Mission> pool, String id) {
        return pool.stream().filter(mission -> mission.id().equals(id)).findFirst().orElseThrow();
    }

    private static MissionBoard.Entry entry(MissionSession session, String id) {
        return session.board().entries().stream().filter(entry -> entry.mission().id().equals(id)).findFirst().orElseThrow();
    }
}
