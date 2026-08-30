package com.morel.greenhouse.infrastructure.repository;

import com.morel.greenhouse.application.dto.AlertDetail;
import com.morel.greenhouse.application.port.GreenhouseRepository;
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
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mock-data")
public class MockGreenhouseRepository implements GreenhouseRepository {

    @Override
    public List<Greenhouse> findGreenhouses() {
        return List.of(
                new Greenhouse(1L, 2L, "A01 羊肚菌智能大棚", "温室一区 / 北侧", GreenhouseStatus.ONLINE, 420.0, "出菇期"),
                new Greenhouse(2L, 2L, "B03 恒湿试验棚", "温室二区 / 东侧", GreenhouseStatus.WARNING, 360.0, "菌丝恢复期"),
                new Greenhouse(3L, null, "C02 低温培育棚", "温室三区 / 西侧", GreenhouseStatus.ONLINE, 500.0, "采收期")
        );
    }

    @Override
    public List<Greenhouse> findGreenhousesByOwner(Long ownerUserId) {
        return findGreenhouses().stream()
                .filter(item -> ownerUserId != null && ownerUserId.equals(item.ownerUserId()))
                .toList();
    }

    @Override
    public Optional<TelemetrySnapshot> findCurrentTelemetry(Long greenhouseId) {
        return Optional.of(new TelemetrySnapshot(
                greenhouseId,
                21.8,
                84.6,
                20.4,
                62.5,
                6.7,
                4280,
                790,
                LocalDateTime.now().minusSeconds(12)
        ));
    }

    @Override
    public List<Device> findDevices(Long greenhouseId) {
        return List.of(
                new Device(1001L, greenhouseId, "循环风机组", "通风", DeviceStatus.RUNNING, "东侧风道", "负责棚内空气循环", true, 96),
                new Device(1002L, greenhouseId, "雾化加湿阵列", "加湿", DeviceStatus.RUNNING, "顶部管线", "维持羊肚菌出菇湿度", true, 92),
                new Device(1003L, greenhouseId, "补光灯带", "补光", DeviceStatus.STOPPED, "中轴棚架", "夜间弱光补偿", false, 89),
                new Device(1004L, greenhouseId, "CO2 调控阀组", "气体", DeviceStatus.MAINTENANCE, "南侧设备间", "二氧化碳联动调节", false, 73)
        );
    }

    @Override
    public List<GreenhouseAlert> findAlerts(Long greenhouseId) {
        return List.of(
                new GreenhouseAlert(2001L, greenhouseId, "B 区湿度波动偏高", "连续 8 分钟超过目标上限 3.5%", AlertLevel.WARNING, AlertStatus.OPEN, LocalDateTime.now().minusMinutes(9)),
                new GreenhouseAlert(2002L, greenhouseId, "补光灯带离线", "设备 1003 已切换为手动巡检状态", AlertLevel.INFO, AlertStatus.ACKNOWLEDGED, LocalDateTime.now().minusHours(1)),
                new GreenhouseAlert(2003L, greenhouseId, "CO2 阀组保养提醒", "建议在 24 小时内完成阀组校准", AlertLevel.CRITICAL, AlertStatus.OPEN, LocalDateTime.now().minusHours(3))
        );
    }

    @Override
    public List<AlertDetail> findAlertDetails(Long greenhouseId) {
        return findAlerts(greenhouseId).stream()
                .map(alert -> new AlertDetail(alert.id(), alert.greenhouseId(), "示例大棚", "温室一区", 1L, "示范农户", null, "未绑定设备",
                        alert.title(), alert.description(), alert.level().name(), alert.status().name(), alert.occurredAt(),
                        null, null, null, null))
                .toList();
    }

    @Override
    public List<TraceabilityRecord> findTraceabilityRecords(Long greenhouseId) {
        return List.of(
                new TraceabilityRecord(3001L, greenhouseId, "ML-202606-A01", "基质入棚", "admin1", LocalDate.now().minusDays(28), "完成批次建档与二维码绑定"),
                new TraceabilityRecord(3002L, greenhouseId, "ML-202606-A01", "温湿度策略调整", "admin2", LocalDate.now().minusDays(12), "夜间湿度目标上调至 86%"),
                new TraceabilityRecord(3003L, greenhouseId, "ML-202606-A01", "巡检采样", "admin1", LocalDate.now().minusDays(2), "抽检 12 个点位，长势均衡")
        );
    }

    @Override
    public Optional<OperatorProfile> findOperator(String username) {
        return Optional.of(new OperatorProfile(1L, username, "系统管理员", "13800000000", "ADMIN", "智慧农业示范基地"));
    }
}
