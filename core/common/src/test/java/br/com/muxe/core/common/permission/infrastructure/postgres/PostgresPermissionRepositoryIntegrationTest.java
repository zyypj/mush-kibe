package br.com.muxe.core.common.permission.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.account.infrastructure.postgres.PostgresAccountDetailsRepository;
import br.com.muxe.core.common.account.infrastructure.postgres.PostgresAccountRepository;
import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesCommand;
import br.com.muxe.core.common.permission.application.command.ApplyPermissionChangesHandler;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionChange;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionRules;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.PermissionFixtures;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgresPermissionRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static final Instant NOW = Instant.parse("2026-10-28T15:00:00Z");

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresPermissionRepository repository = new PostgresPermissionRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "test", new SimpleMetricRegistry());
        new MigrationRunner(database.dataSource()).migrate("core", PostgresPermissionRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void theMigrationCreatesTheRanksTagsAndTracks() {
        PermissionData data = transactions.readOnly("test.load", repository::load);

        assertThat(data.tags().keySet()).containsExactlyInAnyOrderElementsOf(PermissionFixtures.DATA.tags().keySet());
        assertThat(data.groups().keySet()).containsExactlyInAnyOrderElementsOf(PermissionFixtures.RANKS);
        assertThat(data.group("mod").orElseThrow().parents()).containsExactly("partner+");
        assertThat(data.group("mod").orElseThrow().nodes()).extracting(PermissionNode::key).contains("muxe.account.others", "muxe.account.staff");
        assertThat(data.tag("membro").orElseThrow().formatName("tadeu")).isEqualTo("&7tadeu");
        assertThat(data.tag("partner+").orElseThrow().chatPrefix()).isEqualTo("&1&lPARTNER+ ");
        assertThat(data.track("doacao").orElseThrow().groups()).containsExactly("membro", "vip", "mvp", "pro", "ultra", "ultra+");
        assertThat(data.group("ultra+").orElseThrow().parents()).containsExactly("ultra");
        assertThat(data.group("ultra+").orElseThrow().nodes()).extracting(PermissionNode::key).contains("muxe.party.open");
        assertThat(data.group("partner").orElseThrow().parents()).containsExactly("ultra+");
        assertThat(data.tag("ultra+").orElseThrow().chatPrefix()).isEqualTo("&d&lULTRA&6+ ");
        assertThat(data.tag("ultra+").orElseThrow().display()).isEqualTo("&dUltra&6+");
        assertThat(PermissionRules.check(data)).isEmpty();
    }

    @Test
    void aGroupIsSavedWithItsNodesReplaced() {
        Group group = new Group("teste", "Teste", 5, Optional.of("vip"), List.of(
            PermissionNode.permanent("muxe.fly", true, NOW),
            new PermissionNode("muxe.build", false, Contexts.of(Contexts.SERVER, "lobby"), Optional.of(NOW.plus(Duration.ofDays(1))), NOW, NOW)
        ));
        transactions.inTransaction("test.save", () -> {
            repository.saveGroup(group);
            repository.saveGroup(group.withNodes(List.of(group.nodes().getLast())));
            return null;
        });

        Group saved = transactions.readOnly("test.load", repository::load).group("teste").orElseThrow();

        assertThat(saved.nodes()).containsExactly(group.nodes().getLast());
        transactions.inTransaction("test.delete", () -> {
            repository.deleteGroup("teste");
            return null;
        });
        assertThat(transactions.readOnly("test.load", repository::load).group("teste")).isEmpty();
    }

    @Test
    void aUserKeepsNodesAndTheChosenTag() {
        UUID player = UUID.randomUUID();
        PermissionUser user = new PermissionUser(player, List.of(PermissionNode.group("vip", Optional.of(NOW.plus(Duration.ofDays(30))), NOW)), Optional.of("natal"));
        transactions.inTransaction("test.save", () -> {
            repository.saveUser(user);
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.user(player))).isEqualTo(user);
        assertThat(transactions.readOnly("test.find", () -> repository.usersWith("group.vip"))).contains(player);

        transactions.inTransaction("test.clear", () -> {
            repository.selectTag(player, Optional.empty());
            return null;
        });
        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).selectedTag()).isEmpty();
    }

    @Test
    void deletingATagClearsWhoChoseIt() {
        UUID player = UUID.randomUUID();
        transactions.inTransaction("test.save", () -> {
            repository.saveTag(new Tag("pascoa", "Pascoa", "&e", 85, Optional.empty(), true));
            repository.saveTrack(new Track("eventos", List.of("vip")));
            repository.selectTag(player, Optional.of("pascoa"));
            repository.deleteTag("pascoa");
            repository.deleteTrack("eventos");
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).selectedTag()).isEmpty();
        assertThat(transactions.readOnly("test.load", repository::load).track("eventos")).isEmpty();
    }

    @Test
    void theMigrationCreatesTheMedalsAndAPlayerKeepsTheChosenOne() {
        PermissionData data = transactions.readOnly("test.load", repository::load);
        UUID player = UUID.randomUUID();

        assertThat(data.medals().keySet()).containsExactlyInAnyOrderElementsOf(PermissionFixtures.DATA.medals().keySet());
        assertThat(data.medal("feiticeiro-aprendiz").orElseThrow().symbol()).isEqualTo("⊰⊱");
        assertThat(data.medal("casa-de-gengibre").orElseThrow().symbol()).isEqualTo("۩");
        assertThat(data.group("vip").orElseThrow().nodes()).extracting(PermissionNode::key).contains("muxe.medal.carinha", "muxe.medal.toxico", "muxe.medal.yin-yang", PermissionFixtures.LOBBY_FLY, PermissionFixtures.AFK_BYPASS);

        transactions.inTransaction("test.medal", () -> {
            repository.saveMedal(new Medal("pascoa", "Páscoa", "✿", "&d", 400));
            repository.selectMedal(player, Optional.of("pascoa"));
            return null;
        });
        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).selectedMedal()).contains("pascoa");

        transactions.inTransaction("test.delete", () -> {
            repository.deleteMedal("pascoa");
            return null;
        });
        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).selectedMedal()).isEmpty();
    }

    @Test
    void actionsAreListedNewestFirst() {
        transactions.inTransaction("test.log", () -> {
            repository.log(new PermissionAction(NOW, "tadeu", "primeira"));
            repository.log(new PermissionAction(NOW.plusSeconds(1), "tadeu", "segunda"));
            return null;
        });

        assertThat(transactions.readOnly("test.recent", () -> repository.recentActions(2))).extracting(PermissionAction::description).containsExactly("segunda", "primeira");
    }

    @Test
    void accountsGetANumberAndALocation() {
        UUID player = UUID.randomUUID();
        Instant at = NOW.truncatedTo(ChronoUnit.MILLIS);
        PostgresAccountDetailsRepository details = new PostgresAccountDetailsRepository();
        AccountLocation location = new AccountLocation(Optional.of("Brasil"), Optional.of("Minas Gerais"), Optional.of("Tupaciguara"), Optional.of("AS264207"));
        transactions.inTransaction("test.account", () -> {
            new PostgresAccountRepository().save(Account.firstLogin(player, "tadeu", true, at));
            return null;
        });

        assertThat(transactions.readOnly("test.details", () -> details.findById(player))).hasValueSatisfying(found -> {
            assertThat(found.number()).isPositive();
            assertThat(found.location()).isEmpty();
        });

        transactions.inTransaction("test.location", () -> {
            details.saveLocation(player, location, at);
            return null;
        });

        assertThat(transactions.readOnly("test.details", () -> details.findByUsername("TADEU"))).hasValueSatisfying(found -> {
            assertThat(found.account().id()).isEqualTo(player);
            assertThat(found.location()).contains(location);
        });
    }

    @Test
    void manyUsersAreSavedTogetherWithTheirTagsAndMedalsReplaced() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        transactions.inTransaction("test.save", () -> {
            repository.saveUsers(List.of(
                new PermissionUser(first, List.of(PermissionNode.permanent("muxe.fly", true, NOW)), Optional.of("natal")),
                new PermissionUser(second, List.of(PermissionNode.permanent("muxe.build", true, NOW)), Optional.of("natal"))
            ));
            return null;
        });
        PermissionUser firstAgain = new PermissionUser(first, List.of(), Optional.empty());
        PermissionUser secondAgain = new PermissionUser(second, List.of(PermissionNode.permanent("muxe.chat", true, NOW)), Optional.of("natal"));

        transactions.inTransaction("test.save", () -> {
            repository.saveUsers(List.of(firstAgain, secondAgain));
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.user(first))).isEqualTo(firstAgain);
        assertThat(transactions.readOnly("test.find", () -> repository.user(second))).isEqualTo(secondAgain);
    }

    @Test
    void aUserCanChooseATagCreatedLaterInTheSameBatch() {
        UUID player = UUID.randomUUID();
        Tag tag = new Tag("lote", "Lote", "&6", 3, Optional.empty(), true);
        ApplyPermissionChangesHandler handler = new ApplyPermissionChangesHandler(repository, transactions, Clock.fixed(NOW, ZoneOffset.UTC));

        Result<Void> result = handler.handle(new ApplyPermissionChangesCommand("teste", List.of(
            new PermissionChange.PutUser(new PermissionUser(player, List.of(), Optional.of("lote"))),
            new PermissionChange.PutTag(tag)
        )));

        assertThat(result.isSuccess()).isTrue();
        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).selectedTag()).contains("lote");
        transactions.inTransaction("test.cleanup", () -> {
            repository.deleteTag("lote");
            return null;
        });
        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).selectedTag()).isEmpty();
    }

    @Test
    void aFailedBatchRollsBackEverySavedUser() {
        UUID player = UUID.randomUUID();
        PermissionNode node = PermissionNode.permanent("muxe.fly", true, NOW);

        assertThatThrownBy(() -> transactions.inTransaction("test.save", () -> {
            repository.saveUsers(List.of(new PermissionUser(player, List.of(node, node), Optional.empty())));
            return null;
        })).isInstanceOf(PersistenceException.class);

        assertThat(transactions.readOnly("test.find", () -> repository.user(player)).nodes()).isEmpty();
    }
}
