package br.com.muxe.core.common.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesCommand;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesHandler;
import br.com.muxe.core.common.permission.application.command.SelectMedalCommand;
import br.com.muxe.core.common.permission.application.command.SelectMedalHandler;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryPermissionRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MedalTest {

    private static final Instant NOW = Instant.parse("2026-10-28T18:00:00Z");
    private static final Contexts LOBBY = Contexts.server("lobby", "lobby");

    private final UUID player = UUID.randomUUID();

    @Test
    void aMedalShowsItsSymbolInItsColorBeforeTheName() {
        Medal cafe = PermissionFixtures.DATA.medal("cafe").orElseThrow();

        assertThat(cafe.display()).isEqualTo("&e☕");
        assertThat(cafe.chatPrefix()).isEqualTo("&e☕ ");
        assertThat(cafe.permission()).isEqualTo("muxe.medal.cafe");
        assertThat(PermissionFixtures.DATA.medal("feiticeiro-aprendiz").orElseThrow().symbol()).isEqualTo("⊰⊱");
        assertThatThrownBy(() -> new Medal("x", "X", "", "&a", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Medal("x", "X", "abcde", "&a", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Medal("x", "X", "☕", "&z", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theListFollowsTheOrderOfTheMedalsAndHasTheShopOnes() {
        assertThat(PermissionFixtures.DATA.medalsByWeight()).extracting(Medal::symbol)
            .startsWith("☣", "☯", "♫", "⚡", "✌", "❤", "☕", "ツ", "✝", "✽", "∅", "۩", "⚠", "⚛", "℧", "☃", "✴", "⊰⊱", "۞", "✶")
            .contains("火", "水", "気", "✯", "☘", "回", "✈", "✆", "✿", "⚔", "⚜")
            .hasSize(31);
    }

    @Test
    void vipGetsThreeMedalsAndUltraGetsEight() {
        assertThat(resolve(user(PermissionNode.group("vip", Optional.empty(), NOW))).medals()).extracting(Medal::id)
            .containsExactly("toxico", "yin-yang", "carinha");
        assertThat(resolve(user(PermissionNode.group("ultra", Optional.empty(), NOW))).medals()).extracting(Medal::id)
            .containsExactly("toxico", "yin-yang", "musico", "flash", "paz-e-amor", "amor", "cafe", "carinha");
        assertThat(resolve(user()).medals()).isEmpty();
        assertThat(resolve(user(PermissionNode.group("admin", Optional.empty(), NOW))).medals()).hasSize(31);
    }

    @Test
    void aMedalCanBeGivenAloneAndOnlyAnOwnedMedalIsShown() {
        ResolvedPermissions resolved = resolve(user(PermissionNode.permanent("muxe.medal.natal", true, NOW)));

        assertThat(resolved.medals()).extracting(Medal::id).containsExactly("natal");
        assertThat(resolved.displayMedal(Optional.of("natal")).map(Medal::display)).contains("&b✽");
        assertThat(resolved.displayMedal(Optional.of("cafe"))).isEmpty();
        assertThat(resolved.displayMedal(Optional.empty())).isEmpty();
    }

    @Test
    void aMedalIsSelectedAndClearedAndMustExist() {
        InMemoryPermissionRepository repository = InMemoryPermissionRepository.seeded();
        SelectMedalHandler select = new SelectMedalHandler(repository, new DirectTransactions());

        assertThat(select.handle(new SelectMedalCommand(player, Optional.of("cafe"))).isSuccess()).isTrue();
        assertThat(repository.user(player).selectedMedal()).contains("cafe");
        assertThat(select.handle(new SelectMedalCommand(player, Optional.of("nada"))).isFailure()).isTrue();
        assertThat(select.handle(new SelectMedalCommand(player, Optional.empty())).isSuccess()).isTrue();
        assertThat(repository.user(player).selectedMedal()).isEmpty();
    }

    @Test
    void theEditorCreatesAndDeletesMedalsAndDeletingClearsTheChoice() {
        InMemoryPermissionRepository repository = InMemoryPermissionRepository.seeded();
        ApplyPermissionChangesHandler apply = new ApplyPermissionChangesHandler(repository, new DirectTransactions(), new MutableClock(NOW));
        Medal pascoa = new Medal("pascoa", "Páscoa", "✿", "&d", 400);
        repository.selectMedal(player, Optional.of("cafe"));

        assertThat(apply.handle(new ApplyPermissionChangesCommand("tadeu", List.of(new PermissionChange.PutMedal(pascoa), new PermissionChange.DeleteMedal("cafe")))).isSuccess()).isTrue();

        assertThat(repository.load().medal("pascoa")).contains(pascoa);
        assertThat(repository.load().medal("cafe")).isEmpty();
        assertThat(repository.user(player).selectedMedal()).isEmpty();
        assertThat(repository.recentActions(2)).extracting(PermissionAction::description).containsExactly("deleted medal cafe", "saved medal pascoa (&d✿)");
    }

    @Test
    void aUserSavedWithAnUnknownMedalIsRefused() {
        PermissionUser broken = new PermissionUser(player, List.of(), Optional.empty(), Optional.of("nada"));

        assertThat(PermissionRules.checkUser(PermissionFixtures.DATA, broken)).hasValueSatisfying(problem ->
            assertThat(problem.code()).isEqualTo(PermissionProblems.UNKNOWN_MEDAL));
    }

    @Test
    void changingNodesKeepsTheChosenMedal() {
        PermissionUser chosen = new PermissionUser(player, List.of(), Optional.of("vip"), Optional.of("cafe"));

        assertThat(chosen.with(PermissionNode.group("vip", Optional.empty(), NOW)).selectedMedal()).contains("cafe");
        assertThat(chosen.without("group.vip", Contexts.NONE).selectedMedal()).contains("cafe");
        assertThat(chosen.withoutExpired(NOW).selectedMedal()).contains("cafe");
    }

    private ResolvedPermissions resolve(PermissionUser user) {
        return PermissionResolver.resolve(PermissionFixtures.DATA, user, LOBBY, NOW);
    }

    private PermissionUser user(PermissionNode... nodes) {
        return new PermissionUser(player, List.of(nodes), Optional.empty());
    }
}
