package br.com.muxe.core.common.permission.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.permission.application.command.AddUserNodeCommand;
import br.com.muxe.core.common.permission.application.command.AddUserNodeHandler;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesCommand;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesHandler;
import br.com.muxe.core.common.permission.application.command.RemoveUserNodeCommand;
import br.com.muxe.core.common.permission.application.command.RemoveUserNodeHandler;
import br.com.muxe.core.common.permission.application.command.SelectTagCommand;
import br.com.muxe.core.common.permission.application.command.SelectTagHandler;
import br.com.muxe.core.common.permission.application.query.FindUsersWithNodeHandler;
import br.com.muxe.core.common.permission.application.query.FindUsersWithNodeQuery;
import br.com.muxe.core.common.permission.application.query.RecentPermissionActionsHandler;
import br.com.muxe.core.common.permission.application.query.RecentPermissionActionsQuery;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionProblems;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryPermissionRepository;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionHandlersTest {

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");

    private final InMemoryPermissionRepository repository = InMemoryPermissionRepository.seeded();
    private final DirectTransactions transactions = new DirectTransactions();
    private final MutableClock clock = new MutableClock(NOW);
    private final UUID player = UUID.randomUUID();

    @Test
    void aBatchOfChangesIsSavedAndLogged() {
        Tag tag = new Tag("halloween", "Halloween", "&6", 95, Optional.empty(), true);

        Result<Void> result = apply(new PermissionChange.PutTag(tag), new PermissionChange.DeleteTrack("staff"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.load().tag("halloween")).contains(tag);
        assertThat(repository.load().track("staff")).isEmpty();
        assertThat(new RecentPermissionActionsHandler(repository, transactions).handle(new RecentPermissionActionsQuery(10)))
            .extracting(PermissionAction::description)
            .containsExactly("deleted track staff", "saved tag halloween (&6Halloween)");
    }

    @Test
    void aBatchThatBreaksTheRulesSavesNothing() {
        Result<Void> result = apply(new PermissionChange.PutTag(new Tag("x", "X", "&a", 1, Optional.empty(), true)), new PermissionChange.DeleteGroup("membro"));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void>) result).problem().code()).isEqualTo(PermissionProblems.DEFAULT_GROUP_REQUIRED);
        assertThat(repository.load().tag("x")).isEmpty();
    }

    @Test
    void aTemporaryRankIsAddedAndRemoved() {
        PermissionNode vip = PermissionNode.group("vip", Optional.of(NOW.plusSeconds(3600)), NOW);

        Result<PermissionUser> added = new AddUserNodeHandler(repository, transactions, clock).handle(new AddUserNodeCommand("Console", player, vip));
        Result<PermissionUser> removed = new RemoveUserNodeHandler(repository, transactions, clock).handle(new RemoveUserNodeCommand("Console", player, "group.vip", Contexts.NONE));
        Result<PermissionUser> missing = new RemoveUserNodeHandler(repository, transactions, clock).handle(new RemoveUserNodeCommand("Console", player, "group.vip", Contexts.NONE));

        assertThat(added.orElseThrow().nodes()).containsExactly(vip);
        assertThat(new FindUsersWithNodeHandler(repository, transactions).handle(new FindUsersWithNodeQuery("group.vip"))).isEmpty();
        assertThat(removed.orElseThrow().nodes()).isEmpty();
        assertThat(missing.isFailure()).isTrue();
    }

    @Test
    void anUnknownRankCannotBeGiven() {
        Result<PermissionUser> result = new AddUserNodeHandler(repository, transactions, clock)
            .handle(new AddUserNodeCommand("Console", player, PermissionNode.group("lenda", Optional.empty(), NOW)));

        assertThat(result.isFailure()).isTrue();
        assertThat(repository.user(player).nodes()).isEmpty();
    }

    @Test
    void aTagCanBeSelectedAndCleared() {
        SelectTagHandler select = new SelectTagHandler(repository, transactions);

        assertThat(select.handle(new SelectTagCommand(player, Optional.of("natal"))).isSuccess()).isTrue();
        assertThat(repository.user(player).selectedTag()).contains("natal");
        assertThat(select.handle(new SelectTagCommand(player, Optional.of("nada"))).isFailure()).isTrue();
        assertThat(select.handle(new SelectTagCommand(player, Optional.empty())).isSuccess()).isTrue();
        assertThat(repository.user(player).selectedTag()).isEmpty();
    }

    @Test
    void aUserSavedFromTheEditorMustUseExistingGroups() {
        PermissionUser user = new PermissionUser(player, List.of(PermissionNode.group("lenda", Optional.empty(), NOW)), Optional.empty());

        assertThat(apply(new PermissionChange.PutUser(user)).isFailure()).isTrue();
        assertThat(repository.user(player).nodes()).isEmpty();
    }

    private Result<Void> apply(PermissionChange... changes) {
        return new ApplyPermissionChangesHandler(repository, transactions, clock).handle(new ApplyPermissionChangesCommand("tadeu", List.of(changes)));
    }
}
