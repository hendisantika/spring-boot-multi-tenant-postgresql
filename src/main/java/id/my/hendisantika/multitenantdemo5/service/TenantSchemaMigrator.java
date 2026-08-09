package id.my.hendisantika.multitenantdemo5.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

/**
 * Runs the db/migration scripts once per tenant schema.
 * <p>
 * Spring Boot's own Flyway auto-configuration only ever migrates a single
 * schema, which is not enough here: every tenant owns a schema and each needs
 * the same tables, so it is switched off in application.properties and the
 * migration is driven from here instead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaMigrator {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";

    /**
     * Every schema that is not internal to PostgreSQL. `public` is included on
     * purpose: it backs the default tenant.
     */
    private static final String FIND_TENANT_SCHEMAS = """
            SELECT nspname
            FROM pg_namespace
            WHERE nspname NOT LIKE 'pg\\_%'
              AND nspname <> 'information_schema'
            ORDER BY nspname
            """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Brings tenants that already exist up to date at startup. Runs during bean
     * initialisation, the same point at which Boot would have run Flyway, so
     * migrations finish before anything starts serving requests.
     */
    @PostConstruct
    public void migrateExistingTenants() {
        List<String> schemas = jdbcTemplate.queryForList(FIND_TENANT_SCHEMAS, String.class);
        log.info("Migrating {} tenant schema(s): {}", schemas.size(), schemas);
        schemas.forEach(this::migrate);
    }

    /**
     * Applies any outstanding migrations to a single tenant schema. Flyway keeps
     * a separate flyway_schema_history table inside each schema, so tenants
     * created later still start from V1.
     */
    public void migrate(String schema) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations(MIGRATION_LOCATION)
                // A schema that already holds tables but no history predates
                // Flyway; baseline it rather than failing the whole startup.
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
