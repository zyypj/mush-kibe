package br.com.muxe.core.common.persistence.migration;

public record MigrationReport(String module, int migrationsExecuted, String schemaVersion) {
}
