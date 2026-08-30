package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.AlertDetail;
import com.morel.greenhouse.application.dto.DashboardOverview;
import com.morel.greenhouse.application.dto.ProductionSummary;
import com.morel.greenhouse.application.port.GreenhouseRepository;
import com.morel.greenhouse.domain.alert.AlertStatus;
import com.morel.greenhouse.domain.alert.GreenhouseAlert;
import com.morel.greenhouse.domain.device.Device;
import com.morel.greenhouse.domain.device.DeviceStatus;
import com.morel.greenhouse.domain.greenhouse.Greenhouse;
import com.morel.greenhouse.domain.greenhouse.GreenhouseStatus;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.domain.traceability.TraceabilityRecord;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GreenhouseQueryService {
    private final GreenhouseRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public GreenhouseQueryService(GreenhouseRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Greenhouse> listGreenhouses(CurrentUser currentUser) {
        return visibleGreenhouses(currentUser);
    }

    public DashboardOverview getOverview(Long greenhouseId, CurrentUser currentUser) {
        List<Greenhouse> greenhouses = visibleGreenhouses(currentUser);
        if (greenhouses.isEmpty()) {
            return new DashboardOverview(List.of(), null, List.of(), List.of(), new ProductionSummary(0, 0, 0, 0, 0, "-"));
        }
        Long resolvedGreenhouseId = resolveVisibleGreenhouseId(greenhouseId, greenhouses);
        TelemetrySnapshot telemetry = getTelemetry(resolvedGreenhouseId, currentUser);
        List<Device> devices = repository.findDevices(resolvedGreenhouseId);
        List<GreenhouseAlert> activeAlerts = repository.findAlerts(resolvedGreenhouseId).stream()
                .filter(alert -> alert.status() != AlertStatus.RESOLVED)
                .toList();
        ProductionSummary summary = new ProductionSummary(
                (int) greenhouses.stream().filter(g -> g.status() != GreenhouseStatus.OFFLINE).count(),
                (int) devices.stream().filter(device -> device.status() == DeviceStatus.RUNNING).count(),
                activeAlerts.size(),
                batchCount(greenhouses.stream().map(Greenhouse::id).toList()),
                1680.5,
                "A"
        );
        return new DashboardOverview(greenhouses, telemetry, devices, activeAlerts, summary);
    }

    public TelemetrySnapshot getTelemetry(Long greenhouseId, CurrentUser currentUser) {
        return repository.findCurrentTelemetry(resolveGreenhouseId(greenhouseId, currentUser))
                .orElseThrow(() -> new BusinessException(404, "大棚遥测数据不存在"));
    }

    public List<Device> listDevices(Long greenhouseId, CurrentUser currentUser) {
        return repository.findDevices(resolveGreenhouseId(greenhouseId, currentUser));
    }

    public List<GreenhouseAlert> listAlerts(Long greenhouseId, CurrentUser currentUser) {
        return repository.findAlerts(resolveGreenhouseId(greenhouseId, currentUser));
    }

    public List<AlertDetail> listAlertDetails(Long greenhouseId, CurrentUser currentUser) {
        if (currentUser.admin()) {
            return repository.findAlertDetails(greenhouseId);
        }
        List<Greenhouse> visible = visibleGreenhouses(currentUser);
        if (visible.isEmpty()) {
            return List.of();
        }
        if (greenhouseId != null) {
            Long resolved = resolveVisibleGreenhouseId(greenhouseId, visible);
            return repository.findAlertDetails(resolved);
        }
        return visible.stream()
                .flatMap(item -> repository.findAlertDetails(item.id()).stream())
                .toList();
    }

    public List<TraceabilityRecord> listTraceabilityRecords(Long greenhouseId, CurrentUser currentUser) {
        return repository.findTraceabilityRecords(resolveGreenhouseId(greenhouseId, currentUser));
    }

    public List<Map<String, Object>> listBatches(Long farmerId, Long greenhouseId, String batchNo, String startDate, String endDate, CurrentUser currentUser) {
        List<Greenhouse> visible = currentUser != null && currentUser.admin()
                ? (farmerId == null ? repository.findGreenhouses() : repository.findGreenhousesByOwner(farmerId))
                : visibleGreenhouses(currentUser);
        if (visible.isEmpty()) {
            return List.of();
        }
        List<Long> visibleIds = visible.stream().map(Greenhouse::id).toList();
        if (greenhouseId != null && !visibleIds.contains(greenhouseId)) {
            throw new BusinessException(403, "当前账号无权访问该大棚批次");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT b.*, g.name AS greenhouse_name, u.username AS farmer_username, u.display_name AS farmer_name
                FROM production_batch b
                JOIN greenhouse g ON g.id = b.greenhouse_id
                LEFT JOIN app_user u ON u.id = g.owner_user_id
                WHERE b.deleted = FALSE
                """);
        List<Object> params = new ArrayList<>();
        if (greenhouseId != null) {
            sql.append(" AND b.greenhouse_id = ?");
            params.add(greenhouseId);
        } else {
            sql.append(" AND b.greenhouse_id IN (");
            sql.append("?,".repeat(visibleIds.size()));
            sql.setLength(sql.length() - 1);
            sql.append(")");
            params.addAll(visibleIds);
        }
        if (batchNo != null && !batchNo.isBlank()) {
            sql.append(" AND b.batch_no LIKE ?");
            params.add("%" + batchNo.trim() + "%");
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND b.started_at >= ?");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND b.started_at <= ?");
            params.add(endDate.trim());
        }
        sql.append(" ORDER BY b.started_at DESC, b.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> batchDetail(Long batchId, CurrentUser currentUser) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT b.*, g.name AS greenhouse_name, g.owner_user_id
                FROM production_batch b
                JOIN greenhouse g ON g.id = b.greenhouse_id
                WHERE b.id = ? AND b.deleted = FALSE
                """, batchId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "批次不存在");
        }
        Map<String, Object> batch = rows.get(0);
        if (currentUser != null && !currentUser.admin()) {
            Long greenhouseId = Long.valueOf(String.valueOf(batch.get("greenhouse_id")));
            resolveVisibleGreenhouseId(greenhouseId, visibleGreenhouses(currentUser));
        }
        List<Map<String, Object>> events = jdbcTemplate.queryForList("""
                SELECT *
                FROM production_batch_event
                WHERE batch_id = ? AND deleted = FALSE
                ORDER BY sort_order, event_time
                """, batchId);
        return Map.of("batch", batch, "events", events);
    }

    private Long resolveGreenhouseId(Long greenhouseId, CurrentUser currentUser) {
        return resolveVisibleGreenhouseId(greenhouseId, visibleGreenhouses(currentUser));
    }

    private int batchCount(List<Long> greenhouseIds) {
        if (greenhouseIds.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM production_batch WHERE deleted = FALSE AND greenhouse_id IN (");
        sql.append("?,".repeat(greenhouseIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, greenhouseIds.toArray());
        return count == null ? 0 : count.intValue();
    }

    private Long resolveVisibleGreenhouseId(Long greenhouseId, List<Greenhouse> visibleGreenhouses) {
        if (greenhouseId != null && visibleGreenhouses.stream().anyMatch(item -> item.id().equals(greenhouseId))) {
            return greenhouseId;
        }
        if (greenhouseId != null) {
            throw new BusinessException(403, "当前账号无权访问该大棚数据");
        }
        return visibleGreenhouses.stream()
                .findFirst()
                .map(Greenhouse::id)
                .orElseThrow(() -> new BusinessException(404, "当前账号暂未绑定大棚"));
    }

    private List<Greenhouse> visibleGreenhouses(CurrentUser currentUser) {
        if (currentUser == null || currentUser.admin()) {
            return repository.findGreenhouses();
        }
        return repository.findGreenhousesByOwner(currentUser.id());
    }
}
