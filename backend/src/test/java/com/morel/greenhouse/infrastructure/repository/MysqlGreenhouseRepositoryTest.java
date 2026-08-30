package com.morel.greenhouse.infrastructure.repository;

import com.morel.greenhouse.domain.device.DeviceStatus;
import com.morel.greenhouse.domain.greenhouse.GreenhouseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MysqlGreenhouseRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private MysqlGreenhouseRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        repository = new MysqlGreenhouseRepository(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsGreenhousesTelemetryDevicesAlertsTraceabilityAndOperator() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(greenhouseRow(), 0)));
        assertEquals(GreenhouseStatus.ONLINE, repository.findGreenhouses().get(0).status());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(greenhouseRowWithNullOwner(), 0)));
        assertEquals(null, repository.findGreenhousesByOwner(2L).get(0).ownerUserId());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(telemetryRow(), 0)));
        assertTrue(repository.findCurrentTelemetry(1L).isPresent());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(deviceRow("ONLINE"), 0)));
        assertEquals(DeviceStatus.RUNNING, repository.findDevices(1L).get(0).status());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(alertRow(), 0)));
        assertEquals("hot", repository.findAlerts(1L).get(0).title());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(alertDetailRow(), 0)));
        assertEquals("G1", repository.findAlertDetails(1L).get(0).greenhouseName());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(alertDetailRowWithDeviceAndHandledTimes(), 0)));
        assertEquals(10L, repository.findAlertDetails(1L).get(0).deviceId());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(traceabilityRow(), 0)));
        assertEquals(LocalDate.of(2026, 1, 2), repository.findTraceabilityRecords(1L).get(0).operationDate());

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(operatorRow(), 0)));
        assertEquals("admin", repository.findOperator("admin").orElseThrow().username());
    }

    @Test
    void deviceStatusMapperCoversAliasesAndFallback() throws Exception {
        var method = MysqlGreenhouseRepository.class.getDeclaredMethod("mapDeviceStatus", String.class);
        method.setAccessible(true);

        assertEquals(DeviceStatus.STOPPED, method.invoke(repository, (String) null));
        assertEquals(DeviceStatus.MAINTENANCE, method.invoke(repository, "repair"));
        assertEquals(DeviceStatus.STOPPED, method.invoke(repository, "disabled"));
        assertEquals(DeviceStatus.STOPPED, method.invoke(repository, "unknown"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void optionalQueriesReturnEmptyWhenDatabaseReturnsNoRows() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        assertTrue(repository.findCurrentTelemetry(404L).isEmpty());
        assertTrue(repository.findOperator("missing").isEmpty());
    }

    private ResultSet greenhouseRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getObject("owner_user_id")).thenReturn(2L);
        when(rs.getLong("owner_user_id")).thenReturn(2L);
        when(rs.getString("name")).thenReturn("G1");
        when(rs.getString("location")).thenReturn("east");
        when(rs.getString("status")).thenReturn("ONLINE");
        when(rs.getDouble("area")).thenReturn(12.5);
        when(rs.getString("crop_stage")).thenReturn("fruiting");
        return rs;
    }

    private ResultSet greenhouseRowWithNullOwner() throws Exception {
        ResultSet rs = greenhouseRow();
        when(rs.getObject("owner_user_id")).thenReturn(null);
        return rs;
    }

    private ResultSet telemetryRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("greenhouse_id")).thenReturn(1L);
        when(rs.getDouble("air_temperature")).thenReturn(21.5);
        when(rs.getDouble("air_humidity")).thenReturn(82.0);
        when(rs.getDouble("soil_temperature")).thenReturn(18.5);
        when(rs.getDouble("soil_humidity")).thenReturn(62.0);
        when(rs.getDouble("ph_value")).thenReturn(6.7);
        when(rs.getInt("light_lux")).thenReturn(4200);
        when(rs.getInt("co2_ppm")).thenReturn(760);
        when(rs.getTimestamp("collected_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4)));
        return rs;
    }

    private ResultSet deviceRow(String status) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(10L);
        when(rs.getLong("greenhouse_id")).thenReturn(1L);
        when(rs.getString("name")).thenReturn("fan");
        when(rs.getString("category")).thenReturn("VENTILATION");
        when(rs.getString("status")).thenReturn(status);
        when(rs.getString("location")).thenReturn("north");
        when(rs.getString("remark")).thenReturn("ok");
        when(rs.getBoolean("auto_mode")).thenReturn(true);
        when(rs.getInt("health_score")).thenReturn(95);
        return rs;
    }

    private ResultSet alertRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(20L);
        when(rs.getLong("greenhouse_id")).thenReturn(1L);
        when(rs.getString("title")).thenReturn("hot");
        when(rs.getString("description")).thenReturn("too hot");
        when(rs.getString("level")).thenReturn("WARNING");
        when(rs.getString("status")).thenReturn("OPEN");
        when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4)));
        return rs;
    }

    private ResultSet alertDetailRow() throws Exception {
        ResultSet rs = alertRow();
        when(rs.getString("greenhouse_name")).thenReturn("G1");
        when(rs.getString("greenhouse_location")).thenReturn("east");
        when(rs.getObject("farmer_id")).thenReturn(2L);
        when(rs.getLong("farmer_id")).thenReturn(2L);
        when(rs.getString("farmer_name")).thenReturn("farmer");
        when(rs.getObject("device_id")).thenReturn(null);
        when(rs.getString("device_name")).thenReturn(null);
        when(rs.getString("handled_by")).thenReturn("admin");
        when(rs.getString("handle_note")).thenReturn("done");
        when(rs.getTimestamp("handled_at")).thenReturn(null);
        when(rs.getTimestamp("resolved_at")).thenReturn(null);
        return rs;
    }

    private ResultSet alertDetailRowWithDeviceAndHandledTimes() throws Exception {
        ResultSet rs = alertDetailRow();
        when(rs.getObject("farmer_id")).thenReturn(null);
        when(rs.getObject("device_id")).thenReturn(10L);
        when(rs.getLong("device_id")).thenReturn(10L);
        when(rs.getString("device_name")).thenReturn("fan");
        when(rs.getTimestamp("handled_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 4, 5)));
        when(rs.getTimestamp("resolved_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 5, 6)));
        return rs;
    }

    private ResultSet traceabilityRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(30L);
        when(rs.getLong("greenhouse_id")).thenReturn(1L);
        when(rs.getString("batch_no")).thenReturn("B-1");
        when(rs.getString("operation")).thenReturn("plant");
        when(rs.getString("operator")).thenReturn("admin");
        when(rs.getDate("operation_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 2)));
        when(rs.getString("note")).thenReturn("ok");
        return rs;
    }

    private ResultSet operatorRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("username")).thenReturn("admin");
        when(rs.getString("display_name")).thenReturn("Admin");
        when(rs.getString("phone")).thenReturn("13800000000");
        when(rs.getString("email")).thenReturn("a@example.com");
        when(rs.getString("role_code")).thenReturn("ADMIN");
        when(rs.getString("bio")).thenReturn("bio");
        return rs;
    }
}
