package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.port.HardwareGateway;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeviceCommandService {
    private final HardwareGateway hardwareGateway;
    private final JdbcTemplate jdbcTemplate;

    public DeviceCommandService(HardwareGateway hardwareGateway, JdbcTemplate jdbcTemplate) {
        this.hardwareGateway = hardwareGateway;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void execute(DeviceCommandRequest request, CurrentUser currentUser) {
        ensureCanCommandDevice(request.deviceId(), currentUser);
        hardwareGateway.dispatchDeviceCommand(request);
        String command = request.command() == null ? "" : request.command().trim().toUpperCase();
        String nextStatus = switch (command) {
            case "START" -> "RUNNING";
            case "STOP" -> "STOPPED";
            case "MAINTENANCE" -> "MAINTENANCE";
            case "FENGDEGREE" -> fanLevel(request.value()) > 0 ? "RUNNING" : "STOPPED";
            case "LIGHT", "BUMP", "PUMP", "BOARD", "STATE" -> onCommandValue(request.value()) ? "RUNNING" : "STOPPED";
            default -> null;
        };
        if (nextStatus != null) {
            jdbcTemplate.update("""
                    UPDATE greenhouse_device
                    SET status = ?, last_command = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
            """, nextStatus, command, request.deviceId());
        }
    }

    private int fanLevel(String value) {
        if (value == null || value.isBlank()) {
            return 5;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(0, Math.min(9, parsed));
        } catch (NumberFormatException ignored) {
            return 5;
        }
    }

    private boolean onCommandValue(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toUpperCase();
        return !("OFF".equals(normalized) || "STOP".equals(normalized) || "CLOSE".equals(normalized) || "FALSE".equals(normalized) || "0".equals(normalized));
    }

    private void ensureCanCommandDevice(Long deviceId, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM greenhouse_device
                WHERE id = ? AND deleted = FALSE
                """, Integer.class, deviceId);
        if (exists == null || exists == 0) {
            throw new BusinessException(404, "设备不存在");
        }
        if (currentUser.admin()) {
            return;
        }
        Integer allowed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM greenhouse_device d
                JOIN greenhouse g ON g.id = d.greenhouse_id AND g.deleted = FALSE
                LEFT JOIN farmer_greenhouse_binding b ON b.greenhouse_id = g.id AND b.deleted = FALSE
                WHERE d.id = ?
                  AND d.deleted = FALSE
                  AND (g.owner_user_id = ? OR b.farmer_user_id = ?)
                """, Integer.class, deviceId, currentUser.id(), currentUser.id());
        if (allowed == null || allowed == 0) {
            throw new BusinessException(403, "只能调控自己大棚的设备");
        }
    }
}
