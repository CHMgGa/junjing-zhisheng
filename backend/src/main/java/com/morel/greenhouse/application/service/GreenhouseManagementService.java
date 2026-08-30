package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.AlertCommandRequest;
import com.morel.greenhouse.application.dto.CreateBatchEventRequest;
import com.morel.greenhouse.application.dto.CreateBatchRequest;
import com.morel.greenhouse.application.dto.CreateDeviceRequest;
import com.morel.greenhouse.application.dto.CreateGreenhouseRequest;
import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.dto.HandleAlertRequest;
import com.morel.greenhouse.application.dto.UpdateDeviceRequest;
import com.morel.greenhouse.application.dto.UpdateGreenhouseRequest;
import com.morel.greenhouse.domain.device.DeviceStatus;
import com.morel.greenhouse.domain.greenhouse.GreenhouseStatus;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GreenhouseManagementService {
    private static final Logger log = LoggerFactory.getLogger(GreenhouseManagementService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountService userAccountService;
    private final DeviceCommandService deviceCommandService;

    public GreenhouseManagementService(JdbcTemplate jdbcTemplate, UserAccountService userAccountService, DeviceCommandService deviceCommandService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccountService = userAccountService;
        this.deviceCommandService = deviceCommandService;
    }

    @Transactional
    public void createGreenhouse(CreateGreenhouseRequest request, CurrentUser currentUser) {
        Long ownerUserId = currentUser != null && !currentUser.admin() ? currentUser.id() : request.ownerUserId();
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM greenhouse WHERE name = ? AND deleted = FALSE",
                Integer.class,
                request.name()
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "大棚名称已存在");
        }
        jdbcTemplate.update("""
                INSERT INTO greenhouse(owner_user_id, name, location, status, area, crop_stage)
                VALUES (?, ?, ?, 'ONLINE', ?, ?)
                """, ownerUserId, request.name(), request.location(), request.area(), request.cropStage());
        Long greenhouseId = jdbcTemplate.queryForObject("SELECT id FROM greenhouse WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, request.name());
        jdbcTemplate.update("""
                INSERT INTO telemetry_snapshot(
                    greenhouse_id, temperature, humidity, air_temperature, air_humidity,
                    soil_temperature, soil_humidity, ph_value, light_lux, co2_ppm, soil_moisture
                )
                VALUES (?, 20.0, 80.0, 20.0, 80.0, 18.8, 60.0, 6.7, 3600, 760, 60.0)
                """, greenhouseId);
        if (ownerUserId != null) {
            jdbcTemplate.update("""
                    INSERT INTO farmer_greenhouse_binding(farmer_user_id, greenhouse_id)
                    SELECT ?, ?
                    WHERE NOT EXISTS (
                        SELECT 1 FROM farmer_greenhouse_binding WHERE greenhouse_id = ? AND deleted = FALSE
                    )
                    """, ownerUserId, greenhouseId, greenhouseId);
        }
        log.info("Greenhouse created: id={}, name={}", greenhouseId, request.name());
    }

    @Transactional
    public Long createBatch(CreateBatchRequest request, CurrentUser currentUser) {
        if (currentUser == null || !currentUser.admin()) {
            throw new BusinessException(403, "只有管理员可以新建生产批次");
        }
        ensureGreenhouseExists(request.greenhouseId());
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM production_batch WHERE batch_no = ? AND deleted = FALSE",
                Integer.class,
                request.batchNo().trim()
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "批次编号已存在");
        }
        jdbcTemplate.update("""
                INSERT INTO production_batch(greenhouse_id, batch_no, batch_name, crop_name, status, started_at, expected_harvest_at, summary)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.greenhouseId(),
                request.batchNo().trim(),
                request.batchName().trim(),
                blankToDefault(request.cropName(), "羊肚菌"),
                blankToDefault(request.status(), "RUNNING").toUpperCase(),
                request.startedAt(),
                emptyToNull(request.expectedHarvestAt()),
                blankToDefault(request.summary(), "")
        );
        Long batchId = jdbcTemplate.queryForObject("SELECT id FROM production_batch WHERE batch_no = ?", Long.class, request.batchNo().trim());
        createBatchEvent(batchId, new CreateBatchEventRequest(
                "BATCH_CREATED",
                "管理员确认建档",
                "DONE",
                "批次由管理员创建并确认。",
                null
        ), currentUser);
        return batchId;
    }

    @Transactional
    public Long createBatchEvent(Long batchId, CreateBatchEventRequest request, CurrentUser currentUser) {
        if (currentUser == null || !currentUser.admin()) {
            throw new BusinessException(403, "只有管理员可以确认批次链路");
        }
        ensureBatchExists(batchId);
        Integer maxOrder = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM production_batch_event WHERE batch_id = ? AND deleted = FALSE",
                Integer.class,
                batchId
        );
        String previousHash = jdbcTemplate.queryForList("""
                SELECT block_hash
                FROM production_batch_event
                WHERE batch_id = ? AND deleted = FALSE
                ORDER BY sort_order DESC, id DESC
                LIMIT 1
                """, String.class, batchId).stream().findFirst().orElse(null);
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 10;
        String blockHash = "BATCH-" + batchId + "-" + nextOrder + "-" + System.currentTimeMillis();
        jdbcTemplate.update("""
                INSERT INTO production_batch_event(
                    batch_id, event_code, event_title, event_status, operator, event_time,
                    description, image_url, block_hash, previous_hash, sort_order
                )
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)
                """,
                batchId,
                request.eventCode().trim(),
                request.eventTitle().trim(),
                blankToDefault(request.eventStatus(), "DONE").toUpperCase(),
                currentUser.username(),
                blankToDefault(request.description(), ""),
                emptyToNull(request.imageUrl()),
                blockHash,
                previousHash,
                nextOrder
        );
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM production_batch_event
                WHERE batch_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, batchId);
    }

    @Transactional
    public void updateGreenhouse(Long greenhouseId, UpdateGreenhouseRequest request) {
        ensureGreenhouseExists(greenhouseId);
        String status = normalizeGreenhouseStatus(request.status());
        jdbcTemplate.update("""
                UPDATE greenhouse
                SET owner_user_id = ?, name = ?, location = ?, status = ?, area = ?, crop_stage = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, request.ownerUserId(), request.name(), request.location(), status, request.area(), request.cropStage(), greenhouseId);
        if (request.ownerUserId() != null) {
            jdbcTemplate.update("UPDATE farmer_greenhouse_binding SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE greenhouse_id = ?", greenhouseId);
            jdbcTemplate.update("INSERT INTO farmer_greenhouse_binding(farmer_user_id, greenhouse_id) VALUES (?, ?)", request.ownerUserId(), greenhouseId);
        }
    }

    @Transactional
    public void deleteGreenhouse(Long greenhouseId) {
        ensureGreenhouseExists(greenhouseId);
        Integer openAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM greenhouse_alert WHERE greenhouse_id = ? AND status <> 'RESOLVED' AND deleted = FALSE",
                Integer.class,
                greenhouseId
        );
        if (openAlerts != null && openAlerts > 0) {
            throw new BusinessException(409, "该大棚仍有未解决告警，请先完成处理闭环");
        }
        jdbcTemplate.update("UPDATE farmer_greenhouse_binding SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE greenhouse_id = ?", greenhouseId);
        jdbcTemplate.update("UPDATE greenhouse_device SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE greenhouse_id = ?", greenhouseId);
        jdbcTemplate.update("UPDATE greenhouse SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?", greenhouseId);
    }

    @Transactional
    public void createDevice(CreateDeviceRequest request, CurrentUser currentUser) {
        ensureFarmerCanManageGreenhouse(request.greenhouseId(), currentUser);
        jdbcTemplate.update("""
                INSERT INTO greenhouse_device(greenhouse_id, name, category, status, location, remark, auto_mode, health_score)
                VALUES (?, ?, ?, 'STOPPED', ?, ?, ?, 100)
                """, request.greenhouseId(), request.name(), request.category(), request.location(), request.remark(), Boolean.TRUE.equals(request.autoMode()));
    }

    @Transactional
    public void updateDevice(Long deviceId, UpdateDeviceRequest request, CurrentUser currentUser) {
        ensureDeviceExists(deviceId);
        ensureFarmerCanManageGreenhouse(request.greenhouseId(), currentUser);
        Long currentGreenhouseId = jdbcTemplate.queryForObject("SELECT greenhouse_id FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Long.class, deviceId);
        ensureFarmerCanManageGreenhouse(currentGreenhouseId, currentUser);
        jdbcTemplate.update("""
                UPDATE greenhouse_device
                SET greenhouse_id = ?, name = ?, category = ?, status = ?, location = ?, remark = ?, auto_mode = ?, health_score = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, request.greenhouseId(), request.name(), request.category(), normalizeDeviceStatus(request.status()),
                request.location(), request.remark(), Boolean.TRUE.equals(request.autoMode()), request.healthScore(), deviceId);
    }

    @Transactional
    public void deleteDevice(Long deviceId, CurrentUser currentUser) {
        ensureDeviceExists(deviceId);
        Long greenhouseId = jdbcTemplate.queryForObject("SELECT greenhouse_id FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Long.class, deviceId);
        ensureFarmerCanManageGreenhouse(greenhouseId, currentUser);
        Integer openAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM greenhouse_alert WHERE device_id = ? AND status <> 'RESOLVED' AND deleted = FALSE",
                Integer.class,
                deviceId
        );
        if (openAlerts != null && openAlerts > 0) {
            throw new BusinessException(409, "该设备仍有关联未解决告警，请先处理告警后再删除");
        }
        jdbcTemplate.update("UPDATE greenhouse_device SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?", deviceId);
    }

    @Transactional
    public void handleAlert(Long alertId, HandleAlertRequest request, CurrentUser currentUser) {
        ensureAlertVisible(alertId, currentUser);
        if (currentUser == null || currentUser.admin()) {
            throw new BusinessException(403, "管理员只能下发处置建议，告警必须由农户完成解决");
        }
        String nextStatus = request.status().trim().toUpperCase();
        if (!"RESOLVED".equals(nextStatus)) {
            throw new BusinessException(400, "农户处理完成后只能将告警标记为已解决");
        }
        String note = request.note() == null ? "" : request.note().trim();
        if ("RESOLVED".equals(nextStatus) && note.isBlank()) {
            throw new BusinessException(400, "解决告警时需要填写处理意见");
        }
        if (request.deviceId() != null) {
            if (request.command() == null || request.command().isBlank()) {
                throw new BusinessException(400, "请选择调控动作");
            }
            ensureAlertDevice(alertId, request.deviceId());
            deviceCommandService.execute(new DeviceCommandRequest(request.deviceId(), request.command().trim().toUpperCase(), note), currentUser);
        }
        String handler = request.handler() == null || request.handler().isBlank() ? currentUser.username() : request.handler().trim();
        jdbcTemplate.update("""
                UPDATE greenhouse_alert
                SET status = ?, handled_by = ?, handle_note = ?, handled_at = CURRENT_TIMESTAMP,
                    resolved_at = CASE WHEN ? = 'RESOLVED' THEN CURRENT_TIMESTAMP ELSE resolved_at END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, nextStatus, handler, note, nextStatus, alertId);
        if ("RESOLVED".equals(nextStatus) && currentUser != null && !currentUser.admin()) {
            Map<String, Object> alert = alertContext(alertId);
            Long adminId = primaryAdminId();
            String content = "告警已由农户处理完成：《" + alert.get("title") + "》；大棚：" + alert.get("greenhouse_name")
                    + "；设备：" + stringValue(alert.get("device_name"), "大棚环境监测")
                    + "；处理意见：" + note;
            userAccountService.sendSystemMessage(currentUser.id(), adminId, currentUser.id(), adminId, content);
        }
    }

    private void ensureAlertDevice(Long alertId, Long deviceId) {
        Integer allowed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM greenhouse_alert a
                JOIN greenhouse_device d ON d.greenhouse_id = a.greenhouse_id
                WHERE a.id = ?
                  AND d.id = ?
                  AND a.deleted = FALSE
                  AND d.deleted = FALSE
                """, Integer.class, alertId, deviceId);
        if (allowed == null || allowed == 0) {
            throw new BusinessException(403, "只能调控当前告警大棚内的设备");
        }
    }

    @Transactional
    public void recordAlertCommand(Long alertId, AlertCommandRequest request, CurrentUser operatorUser) {
        ensureAlertExists(alertId);
        String operator = operatorUser == null ? "管理员" : operatorUser.username();
        jdbcTemplate.update("""
                INSERT INTO alert_action_log(alert_id, device_id, command, notify_user_id, operator, action_note)
                SELECT ?, ?, ?, g.owner_user_id, ?, ?
                FROM greenhouse_alert a
                JOIN greenhouse g ON g.id = a.greenhouse_id
                WHERE a.id = ?
                """, alertId, request.deviceId(), request.command(), operator, request.note(), alertId);
        jdbcTemplate.update("""
                UPDATE greenhouse_alert
                SET status = CASE WHEN status = 'OPEN' THEN 'ACKNOWLEDGED' ELSE status END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = FALSE
                """, alertId);
        if (Boolean.TRUE.equals(request.notifyFarmer())) {
            Map<String, Object> alert = alertContext(alertId);
            Long farmerId = alert.get("owner_user_id") == null ? null : Long.valueOf(String.valueOf(alert.get("owner_user_id")));
            if (farmerId == null) {
                throw new BusinessException(400, "该告警所属大棚尚未绑定农户，无法通知处置命令");
            }
            Long adminId = operatorUser == null ? primaryAdminId() : operatorUser.id();
            String content = "管理员命令处置：《" + alert.get("title") + "》；农户：" + stringValue(alert.get("farmer_name"), "-")
                    + "；大棚：" + alert.get("greenhouse_name")
                    + "；位置：" + stringValue(alert.get("greenhouse_location"), "-")
                    + "；设备：" + stringValue(alert.get("device_name"), "大棚环境监测")
                    + "；命令：" + request.command()
                    + "；说明：" + stringValue(request.note(), "请尽快查看并处理。");
            userAccountService.sendSystemMessage(farmerId, adminId, adminId, farmerId, content);
        }
    }

    private void ensureFarmerCanManageGreenhouse(Long greenhouseId, CurrentUser currentUser) {
        ensureGreenhouseExists(greenhouseId);
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (currentUser.admin()) {
            throw new BusinessException(403, "管理员只能查看设备，不能新增、修改或删除设备");
        }
        Integer allowed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM farmer_greenhouse_binding
                WHERE greenhouse_id = ? AND farmer_user_id = ? AND deleted = FALSE
                """, Integer.class, greenhouseId, currentUser.id());
        if (allowed == null || allowed == 0) {
            throw new BusinessException(403, "只能管理自己绑定大棚的设备");
        }
    }

    private void ensureAlertVisible(Long alertId, CurrentUser currentUser) {
        ensureAlertExists(alertId);
        if (currentUser == null || currentUser.admin()) {
            return;
        }
        Integer allowed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM greenhouse_alert a
                JOIN farmer_greenhouse_binding b ON b.greenhouse_id = a.greenhouse_id AND b.deleted = FALSE
                WHERE a.id = ? AND b.farmer_user_id = ?
                """, Integer.class, alertId, currentUser.id());
        if (allowed == null || allowed == 0) {
            throw new BusinessException(403, "只能处理自己大棚的告警");
        }
    }

    private Map<String, Object> alertContext(Long alertId) {
        return jdbcTemplate.queryForList("""
                SELECT a.*, g.name AS greenhouse_name, g.location AS greenhouse_location,
                       g.owner_user_id, u.username AS farmer_name, d.name AS device_name
                FROM greenhouse_alert a
                JOIN greenhouse g ON g.id = a.greenhouse_id
                LEFT JOIN app_user u ON u.id = g.owner_user_id
                LEFT JOIN greenhouse_device d ON d.id = a.device_id
                WHERE a.id = ? AND a.deleted = FALSE
                """, alertId).stream().findFirst().orElseThrow(() -> new BusinessException(404, "告警不存在"));
    }

    private Long primaryAdminId() {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM app_user
                WHERE role_code = 'ADMIN' AND enabled = TRUE AND deleted = FALSE
                ORDER BY CASE WHEN username = 'admin1' THEN 0 ELSE 1 END, id
                LIMIT 1
                """, Long.class);
    }

    private void ensureGreenhouseExists(Long greenhouseId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, greenhouseId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "大棚不存在");
        }
    }

    private void ensureDeviceExists(Long deviceId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Integer.class, deviceId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "设备不存在");
        }
    }

    private void ensureAlertExists(Long alertId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, alertId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "告警不存在");
        }
    }

    private void ensureBatchExists(Long batchId) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE id = ? AND deleted = FALSE", Integer.class, batchId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "批次不存在");
        }
    }

    private String normalizeGreenhouseStatus(String status) {
        try {
            return GreenhouseStatus.valueOf(status.trim().toUpperCase()).name();
        } catch (RuntimeException ex) {
            throw new BusinessException(400, "大棚状态只能是在线、预警或离线");
        }
    }

    private String normalizeDeviceStatus(String status) {
        try {
            return DeviceStatus.valueOf(status.trim().toUpperCase()).name();
        } catch (RuntimeException ex) {
            throw new BusinessException(400, "设备状态只能是运行、停止或维护");
        }
    }

    private String stringValue(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
