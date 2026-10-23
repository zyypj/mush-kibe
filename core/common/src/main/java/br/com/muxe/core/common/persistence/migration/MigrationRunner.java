package br.com.muxe.core.common.persistence.migration;

import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;

public final class MigrationRunner {

    public static final String LOCATION_PREFIX = "classpath:db/migration/";
    public static final String HISTORY_TABLE = "schema_history";

    private static final Pattern MODULE = Pattern.compile("[a-z][a-z0-9_]{0,30}");

    private final DataSource dataSource;

    public MigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public MigrationReport migrate(String module, ClassLoader classLoader) {
        if (!MODULE.matcher(module).matches()) {
            throw new IllegalArgumentException("Module name must match " + MODULE.pattern() + ", got: " + module);
        }
        Flyway flyway = Flyway.configure(classLoader)
            .dataSource(dataSource)
            .schemas(module)
            .defaultSchema(module)
            .createSchemas(true)
            .locations(LOCATION_PREFIX + module)
            .table(HISTORY_TABLE)
            .load();
        MigrateResult result = flyway.migrate();
        return new MigrationReport(module, result.migrationsExecuted, currentVersion(flyway, result));
    }

    private static String currentVersion(Flyway flyway, MigrateResult result) {
        if (result.targetSchemaVersion != null) {
            return result.targetSchemaVersion;
        }
        MigrationInfo current = flyway.info().current();
        return current == null ? "none" : current.getVersion().getVersion();
    }
}
