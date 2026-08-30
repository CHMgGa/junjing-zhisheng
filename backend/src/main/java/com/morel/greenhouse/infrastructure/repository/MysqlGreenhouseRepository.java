package com.morel.greenhouse.infrastructure.repository;

import com.morel.greenhouse.application.port.GreenhouseRepository;
import com.morel.greenhouse.application.dto.AlertDetail;
import com.morel.greenhouse.domain.alert.AlertLevel;
import com.morel.greenhouse.domain.alert.AlertStatus;
import com.morel.greenhouse.domain.alert.GreenhouseAlert;
import com.morel.greenhouse.domain.device.Device;
import com.morel.greenhouse.domain.device.DeviceStatus;
import com.morel.greenhouse.domain.greenhouse.Greenhouse;
import com.morel.greenhouse.domain.greenhouse.GreenhouseStatus;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.domain.traceability.TraceabilityRecord;
import com.morel.greenhouse.domain.user.OperatorProfile;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MysqlGreenhouseRepository implements GreenhouseRepository {
    private final JdbcTemplate jdbcTemplate;

    public MysqlGreenhouseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Greenhouse> findGreenhouses() {
        return jdbcTemplate.query("SELECT * FROM greenhouse WHERE deleted = FALSE ORDER BY id", this::mapGreenhouse);
    }

    @Override
    public List<Greenhouse> findGreenhousesByOwner(Long ownerUserId) {
        return jdbcTemplate.query("""
                SELECT g.*
                FROM greenhouse g
                LEFT JOIN farmer_greenhouse_binding b ON b.greenhouse_id = g.id AND b.deleted = FALSE
                WHERE g.deleted = FALSE
                  AND (g.owner_user_id = ? OR b.farmer_user_id = ?)
                GROUP BY g.id, g.owner_user_id, g.name, g.location, g.status, g.area, g.crop_stage,
                         g.deleted, g.created_by, g.updated_by, g.deleted_by, g.created_at, g.updated_at, g.deleted_at
                ORDER BY g.id
                """, this::mapGreenhouse, ownerUserId, ownerUserId);
    }

    @Override
    public Optional<TelemetrySnapshot> findCurrentTelemetry(Long greenhouseId) {
        List<TelemetrySnapshot> rows = jdbcTemplate.query("""
                SELECT * FROM telemetry_snapshot WHERE greenhouse_id = ? ORDER BY collected_at DESC LIMIT 1
                """, this::mapTelemetry, greenhouseId);
        return rows.stream().findFirst();
    }

    @Override
    public List<Device> findDevices(Long greenhouseId) {
        return jdbcTemplate.query("SELECT * FROM greenhouse_device WHERE greenhouse_id = ? AND deleted = FALSE ORDER BY id", this::mapDevice, greenhouseId);
    }

    @Override
    public List<GreenhouseAlert> findAlerts(Long greenhouseId) {
        return jdbcTemplate.query("SELECT * FROM greenhouse_alert WHERE greenhouse_id = ? AND deleted = FALSE ORDER BY occurred_at DESC", this::mapAlert, greenhouseId);
    }

    @Override
    public List<AlertDetail> findAlertDetails(Long greenhouseId) {
        return jdbcTemplate.query("""
                SELECT a.id, a.greenhouse_id, g.name AS greenhouse_name, g.location AS greenhouse_location,
                       u.id AS farmer_id, u.username AS farmer_name,
                       a.device_id, d.name AS device_name,
                       a.title, a.description, a.level, a.status, a.occurred_at,
                       a.handled_by, a.handle_note, a.handled_at, a.resolved_at
                FROM greenhouse_alert a
                JOIN greenhouse g ON g.id = a.greenhouse_id
                LEFT JOIN app_user u ON u.id = g.owner_user_id
                LEFT JOIN greenhouse_device d ON d.id = a.device_id AND d.deleted = FALSE
                WHERE (? IS NULL OR a.greenhouse_id = ?)
                  AND a.deleted = FALSE
                  AND g.deleted = FALSE
                ORDER BY a.occurred_at DESC
                """, this::mapAlertDetail, greenhouseId, greenhouseId);
    }

    @Override
    public List<TraceabilityRecord> findTraceabilityRecords(Long greenhouseId) {
        return jdbcTemplate.query("SELECT * FROM traceability_record WHERE greenhouse_id = ? AND deleted = FALSE ORDER BY operation_date DESC", this::mapTraceability, greenhouseId);
    }

    @Override
    public Optional<OperatorProfile> findOperator(String username) {
        List<OperatorProfile> rows = jdbcTemplate.query("""
                SELECT id, username, display_name, phone, email, role_code, bio FROM app_user WHERE username = ? AND deleted = FALSE
                """, (rs, rowNum) -> new OperatorProfile(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("role_code"),
                rs.getString("bio")
        ), username);
        return rows.stream().findFirst();
    }

    private Greenhouse mapGreenhouse(ResultSet rs, int rowNum) throws SQLException {
        return new Greenhouse(
                rs.getLong("id"),
                rs.getObject("owner_user_id") == null ? null : rs.getLong("owner_user_id"),
                rs.getString("name"),
                rs.getString("location"),
                GreenhouseStatus.valueOf(rs.getString("status")),
                rs.getDouble("area"),
                rs.getString("crop_stage")
        );
    }

    private TelemetrySnapshot mapTelemetry(ResultSet rs, int rowNum) throws SQLException {
        return new TelemetrySnapshot(
                rs.getLong("greenhouse_id"),
                rs.getDouble("air_temperature"),
                rs.getDouble("air_humidity"),
                rs.getDouble("soil_temperature"),
                rs.getDouble("soil_humidity"),
                rs.getDouble("ph_value"),
                rs.getInt("light_lux"),
                rs.getInt("co2_ppm"),
                rs.getTimestamp("collected_at").toLocalDateTime()
        );
    }

    private Device mapDevice(ResultSet rs, int rowNum) throws SQLException {
        return new Device(
                rs.getLong("id"),
                rs.getLong("greenhouse_id"),
                rs.getString("name"),
                rs.getString("category"),
                mapDeviceStatus(rs.getString("status")),
                rs.getString("location"),
                rs.getString("remark"),
                rs.getBoolean("auto_mode"),
                rs.getInt("health_score")
        );
    }

    private GreenhouseAlert mapAlert(ResultSet rs, int rowNum) throws SQLException {
        return new GreenhouseAlert(
                rs.getLong("id"),
                rs.getLong("greenhouse_id"),
                rs.getString("title"),
                rs.getString("description"),
                AlertLevel.valueOf(rs.getString("level")),
                mapAlertStatus(rs.getString("status")),
                rs.getTimestamp("occurred_at").toLocalDateTime()
        );
    }

    private AlertStatus mapAlertStatus(String status) {
        if (status == null || status.isBlank()) {
            return AlertStatus.OPEN;
        }
        return switch (status.trim().toUpperCase()) {
            case "ACTIVE", "PENDING", "UNHANDLED" -> AlertStatus.OPEN;
            case "CONFIRMED", "HANDLING", "HANDLED" -> AlertStatus.ACKNOWLEDGED;
            case "DONE", "CLOSED" -> AlertStatus.RESOLVED;
            default -> AlertStatus.valueOf(status.trim().toUpperCase());
        };
    }

    private AlertDetail mapAlertDetail(ResultSet rs, int rowNum) throws SQLException {
        return new AlertDetail(
                rs.getLong("id"),
                rs.getLong("greenhouse_id"),
                rs.getString("greenhouse_name"),
                rs.getString("greenhouse_location"),
                rs.getObject("farmer_id") == null ? null : rs.getLong("farmer_id"),
                rs.getString("farmer_name"),
                rs.getObject("device_id") == null ? null : rs.getLong("device_id"),
                rs.getString("device_name"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("level"),
                rs.getString("status"),
                rs.getTimestamp("occurred_at").toLocalDateTime(),
                rs.getString("handled_by"),
                rs.getString("handle_note"),
                rs.getTimestamp("handled_at") == null ? null : rs.getTimestamp("handled_at").toLocalDateTime(),
                rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toLocalDateTime()
        );
    }

    private DeviceStatus mapDeviceStatus(String status) {
        if (status == null || status.isBlank()) {
            return DeviceStatus.STOPPED;
        }
        return switch (status.trim().toUpperCase()) {
            case "RUNNING", "ONLINE", "ON" -> DeviceStatus.RUNNING;
            case "MAINTENANCE", "REPAIR" -> DeviceStatus.MAINTENANCE;
            case "STOPPED", "OFF", "OFFLINE", "DISABLED" -> DeviceStatus.STOPPED;
            default -> DeviceStatus.STOPPED;
        };
    }

    private TraceabilityRecord mapTraceability(ResultSet rs, int rowNum) throws SQLException {
        return new TraceabilityRecord(
                rs.getLong("id"),
                rs.getLong("greenhouse_id"),
                rs.getString("batch_no"),
                rs.getString("operation"),
                rs.getString("operator"),
                rs.getDate("operation_date").toLocalDate(),
                rs.getString("note")
        );
    }
}
