package br.com.muxe.minigame.bedwars.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.scoreboard.SidebarContent;
import br.com.muxe.minigame.api.network.ArenaInfo;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.menu.LobbyMenuSettings;
import br.com.muxe.minigame.bedwars.lobby.menu.MapList;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyNpc;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyScoreboardSettings;
import br.com.muxe.minigame.bedwars.lobby.settings.StatsNpcSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopBoardSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopSelectorSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopSelectorView;
import br.com.muxe.minigame.bedwars.lobby.top.TopState;
import br.com.muxe.minigame.bedwars.lobby.top.TopTexts;
import br.com.muxe.minigame.bedwars.lobby.top.TopView;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.stats.domain.PlayerStats;
import br.com.muxe.minigame.bedwars.stats.domain.StatCounters;
import br.com.muxe.minigame.bedwars.stats.domain.StatsPeriod;
import br.com.muxe.minigame.bedwars.stats.domain.TopCategory;
import br.com.muxe.minigame.bedwars.stats.domain.TopEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

class LobbyViewTest {

    private static final LevelBadges BADGES = LevelBadges.from(RoomFixtures.CONFIG.getConfigurationSection("levels"));
    private static final TopTexts TEXTS = TopTexts.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.tops"));
    private static final List<TopBoardSettings> BOARDS = TopBoardSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.tops.boards"));

    @Test
    void theConfiguredLobbyFollowsTheReference() {
        List<LobbyNpc> npcs = LobbyNpc.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.npcs"));
        StatsNpcSettings stats = StatsNpcSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.stats-npc"));

        assertThat(npcs).extracting(LobbyNpc::id).containsExactly("solo", "duplas", "trios", "quartetos", "duelos", "mega", "pilares", "missoes");
        assertThat(npcs).allMatch(npc -> npc.skin().isPresent());
        assertThat(npcs.get(4).players()).containsExactly("1v1", "2v2", "3v3", "4v4", "4s");
        assertThat(npcs.get(5).action()).isEqualTo("mega");
        assertThat(npcs.get(5).players()).containsExactly("mega");
        assertThat(npcs.get(6).action()).isEqualTo("pilares");
        assertThat(npcs.get(6).players()).containsExactly("pilares");
        assertThat(stats.spot().x()).isEqualTo(22.5);
        assertThat(BOARDS).extracting(TopBoardSettings::category).containsExactly(TopCategory.FINAL_KILLS, TopCategory.WINS, TopCategory.WINSTREAK, TopCategory.LEVEL);
        assertThat(BOARDS.getLast().periods()).containsExactly(StatsPeriod.TOTAL, StatsPeriod.MONTHLY, StatsPeriod.WEEKLY);
        assertThat(BOARDS.getLast().modes()).isFalse();
    }

    @Test
    void aTopPageShowsTenPositionsTheClickHintAndThePeriods() {
        List<TopEntry> entries = IntStream.rangeClosed(1, 12)
            .mapToObj(position -> new TopEntry(position, UUID.nameUUIDFromBytes(new byte[] {(byte) position}), "p" + position, 2000 - position, 0))
            .toList();
        TopState state = new TopState();

        List<String> first = TopView.lines(BOARDS.getFirst(), TEXTS, state, entries, id -> "&7", BADGES, mode -> mode.toUpperCase());

        assertThat(first).hasSize(TopView.UPPER_LINES + 2);
        assertThat(first.get(0)).isEqualTo("&b&lTOP 100&r &7(1/10)");
        assertThat(first.get(1)).isEqualTo("&e&lKILLS FINAIS GERAL");
        assertThat(first.get(2)).isEqualTo("&e1.&r &f&7p1&r &7- &e1.999&r");
        assertThat(first.get(12)).isEqualTo("&6&lClique para trocar!");
        assertThat(first.get(13)).isEqualTo("&a&lSemanal&r &7Mensal&r &7Total&r");

        state.nextPage();
        state.nextPage();
        List<String> third = TopView.lines(BOARDS.getFirst(), TEXTS, state, entries, id -> "&d", BADGES, mode -> mode.toUpperCase());

        assertThat(third.get(0)).isEqualTo("&b&lTOP 100&r &7(3/10)");
        assertThat(third.get(2)).isEqualTo("&e21.&r &f&7Ninguém&r &7- &e0&r");
    }

    @Test
    void anEmptyTopListsNobodyWithZero() {
        List<String> lines = TopView.lines(BOARDS.getLast(), TEXTS, new TopState(), List.of(), id -> "&b", BADGES, mode -> mode);

        assertThat(lines.subList(2, 12)).allMatch(line -> line.endsWith("&f&7Ninguém&r &7- &e0&r"));
        assertThat(lines.get(2)).isEqualTo("&e1.&r &f&7Ninguém&r &7- &e0&r");

        TopState monthly = new TopState();
        monthly.nextPeriod(3);
        List<String> xp = TopView.lines(BOARDS.getLast(), TEXTS, monthly, List.of(), id -> "&b", BADGES, mode -> mode);
        assertThat(xp.get(2)).isEqualTo("&e1.&r &f&7Ninguém&r &7- &e0 XP&r");
        assertThat(xp.get(13)).isEqualTo("&7Total&r &a&lMensal&r &7Semanal&r");
        assertThat(TEXTS.clickSound()).contains(Sound.CLICK);
    }

    @Test
    void theModeSelectorFollowsTheExportAndKeepsTheChosenModeVisible() {
        TopSelectorSettings selector = TopSelectorSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.tops.selector")).orElseThrow();
        List<String> order = List.of("geral", "solo", "duplas", "trios", "quartetos", "1v1", "2v2");
        Function<String, String> names = mode -> mode.substring(0, 1).toUpperCase() + mode.substring(1);

        assertThat(selector.header()).isEqualTo("&b&lSELECIONAR MODO");
        assertThat(selector.footer()).isEqualTo("&eClique para alterar.");
        assertThat(TopSelectorView.modes(selector, order, "geral", names)).containsExactly("&aGeral", "&7Solo", "&7Duplas", "&7Trios", "&7Quartetos");
        assertThat(TopSelectorView.modes(selector, order, "trios", names)).containsExactly("&7Geral", "&7Solo", "&7Duplas", "&aTrios", "&7Quartetos");
        assertThat(TopSelectorView.modes(selector, order, "2v2", names)).containsExactly("&7Duplas", "&7Trios", "&7Quartetos", "&71v1", "&a2v2");
    }

    @Test
    void theLobbyScoreboardFollowsTheReference() {
        LobbyScoreboardSettings settings = LobbyScoreboardSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.scoreboard"));
        List<String> modes = List.of("solo", "duplas", "trios", "quartetos", "1v1", "2v2", "3v3", "4v4", "4s");
        Function<String, String> names = mode -> mode.equals("solo") ? "Solo" : mode;

        SidebarContent fresh = LobbySidebar.content(settings, PlayerStats.EMPTY, BADGES, modes, names, 652);

        assertThat(fresh.title()).isEqualTo("&b&lBED WARS");
        assertThat(fresh.lines()).containsExactly(
            "",
            "Seu nível: &7[1&l✫&7]",
            "&f[&a&7▮▮▮▮▮▮▮▮▮▮&f] &7(0/5k)",
            "",
            "&e1v1:",
            " Vitórias: &a0",
            " Winstreak: &a0",
            "&eSolo:",
            " Vitórias: &a0",
            " Winstreak: &a0",
            "",
            "Jogadores: &b652",
            "",
            "&amuxe.com.br"
        );
    }

    @Test
    void theScoreboardShowsTheTwoMostPlayedModes() {
        LobbyScoreboardSettings settings = LobbyScoreboardSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.scoreboard"));
        List<String> modes = List.of("solo", "duplas", "trios", "1v1");
        PlayerStats stats = new PlayerStats(Map.of(
            "geral", counters(30, 9, 5_000),
            "trios", counters(12, 4, 0),
            "duplas", counters(15, 3, 0),
            "solo", counters(3, 2, 0)
        ));

        assertThat(LobbySidebar.mostPlayed(settings, stats, modes)).containsExactly("duplas", "trios");
        assertThat(LobbySidebar.mostPlayed(settings, new PlayerStats(Map.of("trios", counters(1, 0, 0))), modes)).containsExactly("trios", "1v1");
        assertThat(LobbySidebar.content(settings, stats, BADGES, modes, mode -> mode, 1).lines()).contains("&eduplas:", " Vitórias: &a3", "Seu nível: &7[2&l✫&7]");
    }

    private static StatCounters counters(long games, long wins, long xp) {
        return new StatCounters(games, 0, 0, 0, 0, 0, 0, 0, 0, wins, 0, 0, 0, xp);
    }

    @Test
    void changingPeriodOrModeGoesBackToTheFirstPage() {
        TopState state = new TopState();
        state.nextPage();

        state.nextPeriod(3);

        assertThat(state.page()).isZero();
        assertThat(TopView.period(BOARDS.getFirst(), state)).isEqualTo(StatsPeriod.MONTHLY);

        state.nextPeriod(3);
        state.nextPeriod(3);
        state.nextPage();
        state.mode("solo");

        assertThat(TopView.period(BOARDS.getFirst(), state)).isEqualTo(StatsPeriod.WEEKLY);
        assertThat(state.page()).isZero();
        assertThat(TopView.lines(BOARDS.getFirst(), TEXTS, state, List.of(), id -> "", BADGES, mode -> "SOLO").get(1)).isEqualTo("&e&lKILLS FINAIS SOLO");
    }

    @Test
    void theLevelTopShowsTheLevelBadgeOfTheTotalXp() {
        TopEntry first = new TopEntry(1, UUID.randomUUID(), "tadeu", 900, 5_000L + 5_000 * 9 + 10_000);

        List<String> lines = TopView.lines(BOARDS.getLast(), TEXTS, new TopState(), List.of(first), id -> "&b", BADGES, mode -> mode);

        assertThat(lines.get(1)).isEqualTo("&e&lNÍVEL");
        assertThat(lines.get(2)).isEqualTo("&e1.&r &f&btadeu&r &7- &e&a[11&l✶&a]&r");
        assertThat(lines.get(13)).isEqualTo("&a&lTotal&r &7Mensal&r &7Semanal&r");
    }

    @Test
    void theLevelTopShowsTheXpEarnedInTheMonthOrWeek() {
        TopEntry first = new TopEntry(1, UUID.randomUUID(), "liyazita", 402_987, 5_000_000L);
        TopState state = new TopState();
        state.nextPeriod(3);

        List<String> monthly = TopView.lines(BOARDS.getLast(), TEXTS, state, List.of(first), id -> "&7", BADGES, mode -> mode);

        assertThat(TopView.period(BOARDS.getLast(), state)).isEqualTo(StatsPeriod.MONTHLY);
        assertThat(monthly.get(1)).isEqualTo("&e&lNÍVEL");
        assertThat(monthly.get(2)).isEqualTo("&e1.&r &f&7liyazita&r &7- &e402.987 XP&r");
        assertThat(monthly.get(13)).isEqualTo("&7Total&r &a&lMensal&r &7Semanal&r");

        state.nextPeriod(3);

        assertThat(TopView.lines(BOARDS.getLast(), TEXTS, state, List.of(first), id -> "&7", BADGES, mode -> mode).get(2)).endsWith("&e402.987 XP&r");
    }

    @Test
    void theLobbyMenusFollowTheExports() {
        LobbyMenuSettings menus = LobbyMenuSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.menus"));

        assertThat(menus.mode().play().slot()).isEqualTo(11);
        assertThat(menus.mode().play().item().material()).isEqualTo(Material.EYE_OF_ENDER);
        assertThat(menus.mode().map().slot()).isEqualTo(15);
        assertThat(menus.mode().training().slot()).isEqualTo(26);
        assertThat(menus.mode().back().slot()).isEqualTo(21);
        assertThat(menus.mode().backModes()).containsExactly("1v1", "2v2", "3v3", "4v4", "4s");
        assertThat(menus.duels().entries()).containsOnlyKeys("1v1", "2v2", "3v3", "4v4", "4s");
        assertThat(menus.duels().entries().get("4s")).isEqualTo(new LobbyMenuSettings.DuelEntry(17, 4));
        assertThat(menus.maps().slots()).hasSize(21).startsWith(10, 11).endsWith(33, 34);
        assertThat(menus.maps().favourite().name()).isEqualTo("§6✰ §b{map}");
        assertThat(menus.maps().back().slot()).isEqualTo(48);
        assertThat(menus.maps().random().slot()).isEqualTo(49);
        assertThat(menus.maps().search().slot()).isEqualTo(53);
        assertThat(menus.topModes().entries()).hasSize(12);
        assertThat(menus.topModes().entries().get("pilares").item().material()).isEqualTo(Material.BEDROCK);
        assertThat(menus.topModes().entries().get("solo").title()).isEqualTo("SOLO");
        assertThat(menus.stats().general().slot()).isEqualTo(4);
        assertThat(menus.stats().entries().get("mega").item().amount()).isEqualTo(64);
        assertThat(menus.stats().lore()).hasSize(17);
    }

    @Test
    void npcLinesCountPlayersAndBlink() {
        assertThat(LobbyLines.npc(List.of("{blink}&lNOVO MODO", "&e{players} jogando"), 1750, "&d")).containsExactly("&d&lNOVO MODO", "&e1.750 jogando");
        assertThat(LobbyLines.blink(List.of("&f", "&d"), 0, 500)).isEqualTo("&f");
        assertThat(LobbyLines.blink(List.of("&f", "&d"), 500, 500)).isEqualTo("&d");
        assertThat(LobbyLines.blink(List.of("&f", "&d"), 1000, 500)).isEqualTo("&f");
    }

    @Test
    void theStatsNpcShowsTheGeneralCounters() {
        StatCounters general = new StatCounters(10, 7, 2, 30, 9, 4, 5240, 3, 1, 1200, 8, 0, 6, 5_000);
        StatsNpcSettings settings = StatsNpcSettings.from(RoomFixtures.CONFIG.getConfigurationSection("lobby.stats-npc"));

        List<String> lines = LobbyLines.stats(settings.lines(), general, BADGES, settings.barSegments());

        assertThat(lines).startsWith("Seu nível: &7[2&l✫&7]", "&f[&a&7▮▮▮▮▮▮▮▮▮▮▮▮&f] &7(0/5k)", "Vitórias: &a1.200", "Kills: &a30", "Kills finais: &a5.240", "Camas quebradas: &a7", "Winstreak: &a0");
    }

    @Test
    void favouriteMapsComeFirstAndTheSearchIgnoresAccents() {
        List<ArenaInfo> arenas = List.of(new ArenaInfo("1v1", "rio", "Rio"), new ArenaInfo("1v1", "canion", "Cânion"), new ArenaInfo("1v1", "aurora", "Aurora"), new ArenaInfo("1v1", "zen", "Zen"));

        assertThat(MapList.order(arenas, Set.of("zen", "rio"), Optional.empty())).extracting(ArenaInfo::name).containsExactly("rio", "zen", "aurora", "canion");
        assertThat(MapList.order(arenas, Set.of(), Optional.of("CAN"))).extracting(ArenaInfo::name).containsExactly("canion");
    }
}
