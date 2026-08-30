package com.morel.greenhouse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile("mysql")
public class DatabaseInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final boolean initializeEnabled;

    public DatabaseInitializer(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${greenhouse.security.admin-default-password}") String adminPassword,
            @Value("${greenhouse.database.initialize-enabled:true}") boolean initializeEnabled
    ) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.initializeEnabled = initializeEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!initializeEnabled) {
            log.info("MySQL schema and seed initialization skipped");
            return;
        }
        log.info("Initializing MySQL schema and seed data");
        if (schemaExists()) {
            log.info("MySQL schema already exists, skipping schema creation");
        } else {
            executeScript("schema", "db/mysql/schema.sql");
        }
        executeScript("seed", "db/mysql/seed.sql");

        log.info("Running MySQL post-initialization migrations");
        migrateDefaultAdmin();
        normalizeLegacyDeviceStatus();
        normalizeLegacyAlertHandling();
        cleanupDebugAndCorruptedDevices();
        ensureDefaultAvatars();
        ensureUserRoleMappings();
        log.info("MySQL schema and seed initialization completed");
    }

    private boolean schemaExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'app_user'
                """, Integer.class);
        return count != null && count > 0;
    }

    private void executeScript(String name, String path) {
        long startedAt = System.currentTimeMillis();
        log.info("Executing MySQL {} script: {}", name, path);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource(path));
        populator.execute(dataSource);
        log.info("MySQL {} script completed in {} ms", name, System.currentTimeMillis() - startedAt);
    }

    private void migrateDefaultAdmin() {
        Integer admin1Exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_user WHERE username = 'admin1' AND deleted = FALSE",
                Integer.class
        );
        Integer legacyAdminExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_user WHERE username = 'admin' AND deleted = FALSE",
                Integer.class
        );
        if ((admin1Exists == null || admin1Exists == 0) && legacyAdminExists != null && legacyAdminExists > 0) {
            jdbcTemplate.update("""
                    UPDATE app_user
                    SET username = 'admin1',
                        display_name = NULL,
                        bio = '平台管理员',
                        allow_admin_delete = FALSE,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE username = 'admin' AND deleted = FALSE
                    """);
            log.info("Migrated default admin username from admin to admin1");
            return;
        }
        if (admin1Exists == null || admin1Exists == 0) {
            jdbcTemplate.update("""
                    INSERT INTO app_user(username, password_hash, role_code, phone, email, display_name, gender, bio, allow_admin_delete, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "admin1",
                    "{bcrypt}" + passwordEncoder.encode(adminPassword),
                    "ADMIN",
                    "13800000000",
                    "admin1@example.com",
                    null,
                    "UNKNOWN",
                    "平台管理员",
                    false,
                    "system"
            );
            log.info("Admin account initialized: username=admin1");
        }
    }

    private void ensureUserRoleMappings() {
        jdbcTemplate.update("""
                INSERT INTO auth_user_role(user_id, role_id)
                SELECT u.id, r.id
                FROM app_user u, auth_role r
                WHERE u.role_code = r.role_code
                  AND u.deleted = FALSE
                  AND NOT EXISTS (SELECT 1 FROM auth_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id)
                """);
    }

    private void ensureDefaultAvatars() {
        int updated = jdbcTemplate.update("""
                UPDATE app_user
                SET avatar_url = CASE
                    WHEN role_code = 'ADMIN' AND gender = 'FEMALE' THEN '/avatars/female_admin.png'
                    WHEN role_code = 'ADMIN' THEN '/avatars/male_admin.png'
                    WHEN role_code = 'FARMER' AND gender = 'FEMALE' THEN '/avatars/female_farmer.png'
                    ELSE '/avatars/male_farmer.jpg'
                END,
                updated_at = CURRENT_TIMESTAMP
                WHERE deleted = FALSE
                  AND (avatar_url IS NULL OR TRIM(avatar_url) = '')
                """);
        if (updated > 0) {
            log.info("Default user avatars assigned: count={}", updated);
        }
    }

    private void normalizeLegacyDeviceStatus() {
        int updated = jdbcTemplate.update("""
                UPDATE greenhouse_device
                SET status = CASE status
                    WHEN 'ON' THEN 'RUNNING'
                    WHEN 'OFF' THEN 'STOPPED'
                    ELSE status
                END
                WHERE status IN ('ON', 'OFF')
                """);
        if (updated > 0) {
            log.info("Normalized legacy device status rows: count={}", updated);
        }
    }

    private void normalizeLegacyAlertHandling() {
        int statusUpdated = jdbcTemplate.update("""
                UPDATE greenhouse_alert
                SET status = CASE UPPER(status)
                    WHEN 'ACTIVE' THEN 'OPEN'
                    WHEN 'PENDING' THEN 'OPEN'
                    WHEN 'UNHANDLED' THEN 'OPEN'
                    WHEN 'CONFIRMED' THEN 'ACKNOWLEDGED'
                    WHEN 'HANDLING' THEN 'ACKNOWLEDGED'
                    WHEN 'HANDLED' THEN 'ACKNOWLEDGED'
                    WHEN 'DONE' THEN 'RESOLVED'
                    WHEN 'CLOSED' THEN 'RESOLVED'
                    ELSE UPPER(status)
                END,
                updated_at = CURRENT_TIMESTAMP
                WHERE UPPER(status) IN (
                    'ACTIVE',
                    'PENDING',
                    'UNHANDLED',
                    'CONFIRMED',
                    'HANDLING',
                    'HANDLED',
                    'DONE',
                    'CLOSED'
                )
                """);
        if (statusUpdated > 0) {
            log.info("Normalized legacy alert status rows: count={}", statusUpdated);
        }

        int updated = jdbcTemplate.update("""
                UPDATE greenhouse_alert
                SET handled_by = NULL,
                    handle_note = NULL,
                    handled_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE status <> 'RESOLVED'
                  AND (
                    handled_by IS NOT NULL
                    OR handle_note IS NOT NULL
                    OR handled_at IS NOT NULL
                  )
                """);
        if (updated > 0) {
            log.info("Cleared legacy handling fields from unresolved alerts: count={}", updated);
        }
    }

    private void cleanupDebugAndCorruptedDevices() {
        int removed = jdbcTemplate.update("""
                UPDATE greenhouse_device
                SET deleted = TRUE,
                    deleted_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    deleted_by = 'system-cleanup'
                WHERE deleted = FALSE
                  AND (
                    name LIKE '%?%'
                    OR category LIKE '%?%'
                    OR location LIKE '%?%'
                    OR remark LIKE '%?%'
                    OR LOWER(name) LIKE 'farmer-device-%'
                    OR LOWER(name) LIKE 'admin-block-test%'
                    OR LOWER(category) IN ('test', 'debug', 'demo')
                  )
                """);
        if (removed > 0) {
            log.warn("Cleaned debug or corrupted device rows: count={}", removed);
        }
    }
}
