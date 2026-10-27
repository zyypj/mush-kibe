package br.com.muxe.core.common.placement.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import br.com.muxe.core.common.shop.domain.Promotion;
import br.com.muxe.core.common.shop.infrastructure.postgres.PostgresPromotionRepository;
import java.time.Duration;
import java.time.Instant;
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
class PostgresPlacementRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static final Position SPAWN = new Position("lobby", 7.5, 33, -9.5, 180, 0);

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresPlacementRepository repository = new PostgresPlacementRepository();
    private final PostgresPromotionRepository promotions = new PostgresPromotionRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "test", new SimpleMetricRegistry());
        new MigrationRunner(database.dataSource()).migrate("core", PostgresPlacementRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void everyKindOfPlacementSurvivesARoundTrip() {
        String group = group();
        NpcPlacement npc = new NpcPlacement(group, "skywars", SPAWN, "&bSkyWars", NpcAction.game("skywars"), "Notch", Optional.of(new SkinTexture("value", "signature")));
        NpcPlacement plain = new NpcPlacement(group, "guia", SPAWN, "Guia", NpcAction.NONE, "", Optional.empty());
        HologramPlacement hologram = new HologramPlacement(group, "boas-vindas", SPAWN, List.of("&b&lMUXE", "", "&eBem-vindo!"));
        RegisterPlacement register = new RegisterPlacement(group, "caixa", SPAWN, Optional.of(new Position("lobby", 8, 33, -10, 0, 0)));
        RegisterPlacement alone = new RegisterPlacement(group, "sem-vendedor", SPAWN, Optional.empty());
        SocialHeadPlacement head = new SocialHeadPlacement(group, "redes", SPAWN);

        save(npc, plain, hologram, register, alone, head);

        Placements found = transactions.readOnly("test.find", () -> repository.findByGroup(group));
        assertThat(found.npcs()).containsExactly(plain, npc);
        assertThat(found.holograms()).containsExactly(hologram);
        assertThat(found.registers()).containsExactly(register, alone);
        assertThat(found.socialHeads()).containsExactly(head);
    }

    @Test
    void aSocialHeadCanBeMovedAndRemoved() {
        String group = group();
        SocialHeadPlacement head = new SocialHeadPlacement(group, "redes", SPAWN);
        SocialHeadPlacement moved = head.movedTo(new Position("lobby", 4, 34, -10, 90, 0));
        save(head, moved);

        assertThat(transactions.readOnly("test.find", () -> repository.findByGroup(group)).socialHeads()).containsExactly(moved);
        assertThat(transactions.inTransaction("test.remove", () -> repository.remove(PlacementKind.SOCIAL_HEAD, group, "redes"))).isTrue();
        assertThat(transactions.readOnly("test.find", () -> repository.findByGroup(group)).socialHeads()).isEmpty();
    }

    @Test
    void savingTwiceUpdatesAndOtherGroupsStayApart() {
        String group = group();
        RegisterPlacement register = new RegisterPlacement(group, "caixa", SPAWN, Optional.of(SPAWN));
        save(register, register.withSeller(Optional.empty()), new RegisterPlacement(group(), "caixa", SPAWN, Optional.empty()));

        assertThat(transactions.readOnly("test.find", () -> repository.findByGroup(group)).registers()).containsExactly(register.withSeller(Optional.empty()));
    }

    @Test
    void removingTellsWhetherSomethingWasThere() {
        String group = group();
        save(new HologramPlacement(group, "oi", SPAWN, List.of("&bOi")));

        assertThat(transactions.inTransaction("test.remove", () -> repository.remove(PlacementKind.HOLOGRAM, group, "oi"))).isTrue();
        assertThat(transactions.inTransaction("test.remove", () -> repository.remove(PlacementKind.HOLOGRAM, group, "oi"))).isFalse();
        assertThat(transactions.inTransaction("test.remove", () -> repository.remove(PlacementKind.NPC, group, "oi"))).isFalse();
    }

    @Test
    void thePromotionIsASingleRowThatChanges() {
        Promotion first = new Promotion("enderlore", List.of("&b&lLOJA", "&eClique para comprar!"));
        Promotion second = new Promotion("vip", List.of());

        transactions.inTransaction("test.promotion", () -> {
            promotions.save(first, Instant.parse("2026-10-26T12:00:00Z"));
            promotions.save(second, Instant.parse("2026-10-26T12:01:00Z"));
            return null;
        });

        assertThat(transactions.readOnly("test.promotion", promotions::find)).contains(second);
    }

    private void save(Placement... placements) {
        transactions.inTransaction("test.save", () -> {
            for (Placement placement : placements) {
                repository.save(placement);
            }
            return null;
        });
    }

    private static String group() {
        return "g" + UUID.randomUUID().toString().substring(0, 8);
    }
}
