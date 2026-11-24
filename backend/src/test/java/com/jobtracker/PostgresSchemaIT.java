package com.jobtracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Migrações Flyway no PostgreSQL 16. Usa um container efêmero: nunca acessa o banco do
 * docker-compose nem dados pessoais. O contexto Spring só sobe se o Hibernate (ddl-auto=validate)
 * aceitar o esquema criado pelas migrações.
 */
@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers
class PostgresSchemaIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4");

    @Autowired JdbcTemplate jdbc;

    @Test
    void emptyDatabaseIsMigratedToLatestVersion() {
        assertEquals(List.of("1", "2", "3"), successfulVersions(jdbc));
        assertEquals(
                1,
                count(
                        jdbc,
                        "select count(*) from information_schema.columns where table_name = 'users' and column_name = 'profile_image'"));
        assertEquals(
                1,
                count(
                        jdbc,
                        "select count(*) from information_schema.columns where table_name = 'companies' and column_name = 'deleted_at'"));
        assertEquals(
                1,
                count(
                        jdbc,
                        "select count(*) from information_schema.columns where table_name = 'job_applications' and column_name = 'deleted_at'"));
    }

    @Test
    void upgradingFromV1ToV3PreservesLegacyDataAndAddsNewColumns() throws Exception {
        String database = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword());
                var statement = admin.createStatement()) {
            statement.execute("create database " + database);
        }
        String url =
                "jdbc:postgresql://"
                        + postgres.getHost()
                        + ":"
                        + postgres.getFirstMappedPort()
                        + "/"
                        + database;

        Flyway.configure()
                .dataSource(url, postgres.getUsername(), postgres.getPassword())
                .target(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        var legacy =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                url, postgres.getUsername(), postgres.getPassword()));
        long userId =
                legacy.queryForObject(
                        "insert into users (name, email, password, role, created_at) values ('Usuario legado', 'legado@example.com', 'hash-de-teste', 'USER', now()) returning id",
                        Long.class);
        long companyId =
                legacy.queryForObject(
                        "insert into companies (name, normalized_name, approval_status, created_by, created_at, updated_at) values ('Empresa legada', 'empresa legada', 'APPROVED', ?, now(), now()) returning id",
                        Long.class,
                        userId);
        long stageId =
                legacy.queryForObject(
                        "insert into application_stages (user_id, name, position, color, is_final, is_active, created_at) values (?, 'Inscrito', 0, '#2563eb', false, true, now()) returning id",
                        Long.class,
                        userId);
        long applicationId =
                legacy.queryForObject(
                        "insert into job_applications (user_id, company_id, stage_id, job_title, applied_at, created_at, updated_at) values (?, ?, ?, 'Vaga legada', date '2026-11-05', now(), now()) returning id",
                        Long.class,
                        userId,
                        companyId,
                        stageId);
        legacy.update(
                "insert into application_history (application_id, new_stage_id, changed_by, notes, changed_at) values (?, ?, ?, 'Candidatura registrada.', now())",
                applicationId,
                stageId,
                userId);
        legacy.update(
                "insert into application_notes (application_id, user_id, content, created_at) values (?, ?, 'Nota antiga', now())",
                applicationId,
                userId);

        Flyway.configure()
                .dataSource(url, postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();

        assertEquals(List.of("1", "2", "3"), successfulVersions(legacy));
        assertEquals(
                1,
                count(
                        legacy,
                        "select count(*) from users where id = ? and email = 'legado@example.com' and profile_image is null",
                        userId));
        assertEquals(
                1,
                count(
                        legacy,
                        "select count(*) from companies where id = ? and deleted_at is null",
                        companyId));
        assertEquals(
                1,
                count(
                        legacy,
                        "select count(*) from job_applications where id = ? and deleted_at is null",
                        applicationId));
        assertEquals(
                1,
                count(
                        legacy,
                        "select count(*) from application_history where application_id = ?",
                        applicationId));
        assertEquals(
                1,
                count(
                        legacy,
                        "select count(*) from application_notes where application_id = ?",
                        applicationId));
    }

    private static List<String> successfulVersions(JdbcTemplate db) {
        return db.queryForList(
                "select version from flyway_schema_history where success order by installed_rank",
                String.class);
    }

    private static int count(JdbcTemplate db, String sql, Object... args) {
        return db.queryForObject(sql, Integer.class, args);
    }
}
