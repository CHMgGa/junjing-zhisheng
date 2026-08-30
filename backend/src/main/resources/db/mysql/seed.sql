INSERT INTO sys_dict_type(type_code, type_name, description, sort_order)
SELECT 'GREENHOUSE_STATUS', '大棚状态', '大棚运行状态字典', 10
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'GREENHOUSE_STATUS');

INSERT INTO sys_dict_type(type_code, type_name, description, sort_order)
SELECT 'DEVICE_STATUS', '设备状态', '设备运行状态字典', 20
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'DEVICE_STATUS');

INSERT INTO sys_dict_type(type_code, type_name, description, sort_order)
SELECT 'ALERT_LEVEL', '告警级别', '告警严重程度字典', 30
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'ALERT_LEVEL');

INSERT INTO sys_dict_type(type_code, type_name, description, sort_order)
SELECT 'ALERT_STATUS', '告警状态', '告警闭环处理状态字典', 40
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'ALERT_STATUS');

UPDATE sys_dict_type SET type_name = '大棚状态', description = '大棚运行状态字典' WHERE type_code = 'GREENHOUSE_STATUS';
UPDATE sys_dict_type SET type_name = '设备状态', description = '设备运行状态字典' WHERE type_code = 'DEVICE_STATUS';
UPDATE sys_dict_type SET type_name = '告警级别', description = '告警严重程度字典' WHERE type_code = 'ALERT_LEVEL';
UPDATE sys_dict_type SET type_name = '告警状态', description = '告警闭环处理状态字典' WHERE type_code = 'ALERT_STATUS';

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'GREENHOUSE_STATUS', 'ONLINE', '在线', 'ONLINE', 'success', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'GREENHOUSE_STATUS' AND item_code = 'ONLINE');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'GREENHOUSE_STATUS', 'WARNING', '预警', 'WARNING', 'warning', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'GREENHOUSE_STATUS' AND item_code = 'WARNING');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'GREENHOUSE_STATUS', 'OFFLINE', '离线', 'OFFLINE', 'info', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'GREENHOUSE_STATUS' AND item_code = 'OFFLINE');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'DEVICE_STATUS', 'RUNNING', '运行中', 'RUNNING', 'success', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'DEVICE_STATUS' AND item_code = 'RUNNING');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'DEVICE_STATUS', 'STOPPED', '已停止', 'STOPPED', 'info', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'DEVICE_STATUS' AND item_code = 'STOPPED');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'DEVICE_STATUS', 'MAINTENANCE', '维护中', 'MAINTENANCE', 'warning', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'DEVICE_STATUS' AND item_code = 'MAINTENANCE');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'ALERT_LEVEL', 'INFO', '提示', 'INFO', 'info', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'ALERT_LEVEL' AND item_code = 'INFO');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'ALERT_LEVEL', 'WARNING', '警告', 'WARNING', 'warning', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'ALERT_LEVEL' AND item_code = 'WARNING');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'ALERT_LEVEL', 'CRITICAL', '严重', 'CRITICAL', 'danger', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'ALERT_LEVEL' AND item_code = 'CRITICAL');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'ALERT_STATUS', 'OPEN', '待处理', 'OPEN', 'danger', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'ALERT_STATUS' AND item_code = 'OPEN');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'ALERT_STATUS', 'ACKNOWLEDGED', '已确认', 'ACKNOWLEDGED', 'warning', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'ALERT_STATUS' AND item_code = 'ACKNOWLEDGED');

INSERT INTO sys_dict_item(type_code, item_code, item_name, item_value, tag_type, sort_order)
SELECT 'ALERT_STATUS', 'RESOLVED', '已解决', 'RESOLVED', 'success', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE type_code = 'ALERT_STATUS' AND item_code = 'RESOLVED');

UPDATE sys_dict_item SET item_name = '在线' WHERE type_code = 'GREENHOUSE_STATUS' AND item_code = 'ONLINE';
UPDATE sys_dict_item SET item_name = '预警' WHERE type_code = 'GREENHOUSE_STATUS' AND item_code = 'WARNING';
UPDATE sys_dict_item SET item_name = '离线' WHERE type_code = 'GREENHOUSE_STATUS' AND item_code = 'OFFLINE';
UPDATE sys_dict_item SET item_name = '运行中' WHERE type_code = 'DEVICE_STATUS' AND item_code = 'RUNNING';
UPDATE sys_dict_item SET item_name = '已停止' WHERE type_code = 'DEVICE_STATUS' AND item_code = 'STOPPED';
UPDATE sys_dict_item SET item_name = '维护中' WHERE type_code = 'DEVICE_STATUS' AND item_code = 'MAINTENANCE';
UPDATE sys_dict_item SET item_name = '提示' WHERE type_code = 'ALERT_LEVEL' AND item_code = 'INFO';
UPDATE sys_dict_item SET item_name = '警告' WHERE type_code = 'ALERT_LEVEL' AND item_code = 'WARNING';
UPDATE sys_dict_item SET item_name = '严重' WHERE type_code = 'ALERT_LEVEL' AND item_code = 'CRITICAL';
UPDATE sys_dict_item SET item_name = '待处理' WHERE type_code = 'ALERT_STATUS' AND item_code = 'OPEN';
UPDATE sys_dict_item SET item_name = '已确认' WHERE type_code = 'ALERT_STATUS' AND item_code = 'ACKNOWLEDGED';
UPDATE sys_dict_item SET item_name = '已解决' WHERE type_code = 'ALERT_STATUS' AND item_code = 'RESOLVED';

INSERT INTO auth_role(role_code, role_name, description, data_scope)
SELECT 'ADMIN', '系统管理员', '拥有用户、大棚、设备、告警和反馈管理权限', 'ALL'
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE role_code = 'ADMIN');

INSERT INTO auth_role(role_code, role_name, description, data_scope)
SELECT 'FARMER', '农户', '查看本人绑定大棚、设备、告警和反馈消息', 'SELF'
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE role_code = 'FARMER');

UPDATE auth_role SET role_name = '系统管理员', description = '拥有用户、大棚、设备、告警和反馈管理权限', data_scope = 'ALL' WHERE role_code = 'ADMIN';
UPDATE auth_role SET role_name = '农户', description = '查看本人绑定大棚、设备、告警和反馈消息', data_scope = 'SELF' WHERE role_code = 'FARMER';

INSERT INTO auth_permission(permission_code, permission_name, resource_type, resource_path, http_method, sort_order)
SELECT 'greenhouse:manage', '大棚管理', 'API', '/api/v1/greenhouses/**', '*', 10
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'greenhouse:manage');

INSERT INTO auth_permission(permission_code, permission_name, resource_type, resource_path, http_method, sort_order)
SELECT 'device:manage', '设备管理', 'API', '/api/v1/greenhouses/devices/**', '*', 20
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'device:manage');

INSERT INTO auth_permission(permission_code, permission_name, resource_type, resource_path, http_method, sort_order)
SELECT 'alert:handle', '告警处理', 'API', '/api/v1/greenhouses/alerts/**', '*', 30
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'alert:handle');

INSERT INTO auth_permission(permission_code, permission_name, resource_type, resource_path, http_method, sort_order)
SELECT 'farmer:workbench', '农户工作台', 'MENU', '/farmer', 'GET', 50
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'farmer:workbench');

INSERT INTO auth_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM auth_role r, auth_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('greenhouse:manage', 'device:manage', 'alert:handle', 'farmer:workbench')
  AND NOT EXISTS (SELECT 1 FROM auth_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO auth_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM auth_role r, auth_permission p
WHERE r.role_code = 'FARMER'
  AND p.permission_code IN ('farmer:workbench')
  AND NOT EXISTS (SELECT 1 FROM auth_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO device_type(type_code, type_name, category, protocol, unit, telemetry_key, command_capable, description)
SELECT 'VENTILATION_FAN', '循环风机组', '通风', 'MODBUS', NULL, 'wind', TRUE, '用于棚内通风换气与空气循环'
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code = 'VENTILATION_FAN');

INSERT INTO device_type(type_code, type_name, category, protocol, unit, telemetry_key, command_capable, description)
SELECT 'HUMIDIFIER', '雾化加湿器', '加湿', 'MODBUS', '%', 'humidity', TRUE, '用于维持羊肚菌生长湿度'
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code = 'HUMIDIFIER');

INSERT INTO device_type(type_code, type_name, category, protocol, unit, telemetry_key, command_capable, description)
SELECT 'CO2_SENSOR', 'CO2 传感器', '环境传感器', 'MODBUS', 'ppm', 'co2_ppm', FALSE, '采集棚内 CO2 浓度'
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code = 'CO2_SENSOR');

INSERT INTO device_type(type_code, type_name, category, protocol, unit, telemetry_key, command_capable, description)
SELECT 'IRRIGATION_PUMP', '灌溉水泵', '灌溉', 'MODBUS', NULL, 'soil_moisture', TRUE, '用于土壤和基质补水'
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code = 'IRRIGATION_PUMP');

UPDATE device_type SET type_name = '循环风机组', category = '通风', description = '用于棚内通风换气与空气循环' WHERE type_code = 'VENTILATION_FAN';
UPDATE device_type SET type_name = '雾化加湿器', category = '加湿', description = '用于维持羊肚菌生长湿度' WHERE type_code = 'HUMIDIFIER';
UPDATE device_type SET type_name = 'CO2 传感器', category = '环境传感器', description = '采集棚内 CO2 浓度' WHERE type_code = 'CO2_SENSOR';
UPDATE device_type SET type_name = '灌溉水泵', category = '灌溉', description = '用于土壤和基质补水' WHERE type_code = 'IRRIGATION_PUMP';

INSERT INTO alert_rule(rule_code, rule_name, metric_key, operator, threshold_value, duration_minutes, level, description)
SELECT 'HUMIDITY_HIGH_A01', '湿度持续偏高', 'humidity', 'GT', 88.00, 8, 'WARNING', '连续多分钟超过湿度上限时触发'
WHERE NOT EXISTS (SELECT 1 FROM alert_rule WHERE rule_code = 'HUMIDITY_HIGH_A01');

INSERT INTO alert_rule(rule_code, rule_name, metric_key, operator, threshold_value, duration_minutes, level, description)
SELECT 'CO2_HIGH_A01', 'CO2 浓度偏高', 'co2_ppm', 'GT', 1200.00, 5, 'CRITICAL', 'CO2 浓度过高时提醒通风'
WHERE NOT EXISTS (SELECT 1 FROM alert_rule WHERE rule_code = 'CO2_HIGH_A01');

UPDATE alert_rule
SET rule_name = '空气湿度偏高',
    metric_key = 'air_humidity',
    operator = 'GT',
    threshold_value = 92.00,
    level = 'WARNING',
    description = '空气湿度过高可能增加病害风险',
    updated_at = CURRENT_TIMESTAMP
WHERE rule_code = 'HUMIDITY_HIGH_A01';
UPDATE alert_rule SET rule_name = 'CO2 浓度偏高', description = 'CO2 浓度过高时提醒通风' WHERE rule_code = 'CO2_HIGH_A01';
UPDATE alert_rule SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE rule_code = 'AIR_HUMIDITY_HIGH';

INSERT IGNORE INTO alert_rule(rule_code, rule_name, metric_key, operator, threshold_value, duration_minutes, level, description)
VALUES ('AIR_TEMP_LOW', '空气温度偏低', 'air_temperature', 'LT', 16.00, 1, 'WARNING', '空气温度低于羊肚菌适宜范围'),
  ('AIR_TEMP_HIGH', '空气温度偏高', 'air_temperature', 'GT', 24.00, 1, 'WARNING', '空气温度高于羊肚菌适宜范围'),
  ('AIR_HUMIDITY_LOW', '空气湿度偏低', 'air_humidity', 'LT', 70.00, 1, 'WARNING', '空气湿度低于羊肚菌适宜范围'),
  ('SOIL_TEMP_LOW', '土壤温度偏低', 'soil_temperature', 'LT', 16.00, 1, 'WARNING', '土壤温度低于羊肚菌适宜范围'),
  ('SOIL_TEMP_HIGH', '土壤温度偏高', 'soil_temperature', 'GT', 24.00, 1, 'WARNING', '土壤温度高于羊肚菌适宜范围'),
  ('SOIL_HUMIDITY_LOW', '土壤湿度偏低', 'soil_humidity', 'LT', 58.00, 1, 'WARNING', '土壤湿度偏低，需关注补水'),
  ('SOIL_HUMIDITY_HIGH', '土壤湿度偏高', 'soil_humidity', 'GT', 72.00, 1, 'WARNING', '土壤湿度偏高，需关注通风和积水'),
  ('PH_LOW', 'pH 值偏低', 'ph_value', 'LT', 6.30, 1, 'WARNING', 'pH 值偏酸，建议复核基质'),
  ('PH_HIGH', 'pH 值偏高', 'ph_value', 'GT', 7.10, 1, 'WARNING', 'pH 值偏碱，建议复核基质'),
  ('CO2_LOW', 'CO2 浓度偏低', 'co2_ppm', 'LT', 450.00, 1, 'INFO', 'CO2 浓度偏低，建议关注通风策略'),
  ('CO2_HIGH', 'CO2 浓度偏高', 'co2_ppm', 'GT', 1000.00, 1, 'CRITICAL', 'CO2 浓度偏高，建议及时通风'),
  ('LIGHT_LOW', '光照强度偏低', 'light_lux', 'LT', 2000.00, 1, 'INFO', '光照强度偏低，建议检查补光策略'),
  ('LIGHT_HIGH', '光照强度偏高', 'light_lux', 'GT', 6000.00, 1, 'WARNING', '光照强度偏高，建议适当遮光');

INSERT INTO app_user(username, password_hash, role_code, phone, email, display_name, gender, bio, created_by)
SELECT 'farmer001', '{bcrypt}$2a$10$JkchLJY4LoYghNpK4kJ5Ae2TQwAotSKfih8CV0McdCrw.7HfMS1MC', 'FARMER', '13900000001', 'farmer001@example.com', '示范农户', 'MALE', '负责 A01 大棚日常巡检和出菇期管理', 'system'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'farmer001');

UPDATE app_user SET display_name = '示范农户', bio = '负责 A01 大棚日常巡检和出菇期管理' WHERE username = 'farmer001';

INSERT INTO auth_user_role(user_id, role_id)
SELECT u.id, r.id FROM app_user u, auth_role r
WHERE u.username = 'farmer001' AND r.role_code = 'FARMER'
  AND NOT EXISTS (SELECT 1 FROM auth_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

UPDATE greenhouse SET name = 'A01 羊肚菌智能大棚', location = '温室一区 / 北侧', crop_stage = '出菇期'
WHERE owner_user_id = (SELECT id FROM app_user WHERE username = 'farmer001' LIMIT 1)
  AND name LIKE 'A01 %';

INSERT INTO greenhouse(owner_user_id, name, location, status, area, crop_stage, created_by)
SELECT u.id, 'A01 羊肚菌智能大棚', '温室一区 / 北侧', 'ONLINE', 420.00, '出菇期', 'system'
FROM app_user u WHERE u.username = 'farmer001'
AND NOT EXISTS (SELECT 1 FROM greenhouse WHERE name = 'A01 羊肚菌智能大棚' AND deleted = FALSE);

INSERT INTO farmer_greenhouse_binding(farmer_user_id, greenhouse_id)
SELECT u.id, g.id FROM app_user u, greenhouse g
WHERE u.username = 'farmer001' AND g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM farmer_greenhouse_binding b WHERE b.greenhouse_id = g.id AND b.deleted = FALSE);

INSERT INTO greenhouse_device(greenhouse_id, device_type_id, name, category, status, location, manufacturer, model, protocol, auto_mode, health_score, created_by)
SELECT g.id, dt.id, '循环风机组', dt.category, 'RUNNING', '东侧风道', 'Morel IoT Lab', 'FAN-MR-2026', dt.protocol, TRUE, 96, 'system'
FROM greenhouse g
JOIN device_type dt ON dt.type_code = 'VENTILATION_FAN'
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_device WHERE name = '循环风机组' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO telemetry_snapshot(greenhouse_id, temperature, humidity, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, light_lux, co2_ppm, soil_moisture)
SELECT g.id, 21.8, 84.6, 21.8, 84.6, 20.6, 62.5, 6.7, 4280, 790, 62.5 FROM greenhouse g
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
  AND NOT EXISTS (
    SELECT 1 FROM telemetry_snapshot ts
    WHERE ts.greenhouse_id = g.id AND ts.temperature = 21.8 AND ts.humidity = 84.6
  );

INSERT INTO greenhouse_alert(greenhouse_id, device_id, rule_id, title, description, level, status, created_by)
SELECT g.id, d.id, r.id, '湿度波动偏高', '连续 8 分钟超过目标上限 3.5%，请检查加湿策略和通风设备。', 'WARNING', 'OPEN', 'system'
FROM greenhouse g
LEFT JOIN greenhouse_device d ON d.greenhouse_id = g.id AND d.name = '循环风机组' AND d.deleted = FALSE
LEFT JOIN alert_rule r ON r.rule_code = 'HUMIDITY_HIGH_A01'
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_alert WHERE title = '湿度波动偏高' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO traceability_record(greenhouse_id, batch_no, operation, operator, operation_date, note, created_by)
SELECT g.id, 'ML-202606-A01', '基质入棚', 'admin1', DATE_SUB(CURRENT_DATE, INTERVAL 28 DAY), '完成批次建档并绑定溯源码', 'system'
FROM greenhouse g WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM traceability_record WHERE batch_no = 'ML-202606-A01' AND operation = '基质入棚' AND deleted = FALSE);

INSERT INTO production_batch(greenhouse_id, batch_no, batch_name, crop_name, status, started_at, expected_harvest_at, summary)
SELECT g.id, 'ML-202606-A01', 'A01 春季羊肚菌示范批次', '羊肚菌', 'RUNNING', DATE_SUB(CURRENT_DATE, INTERVAL 28 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), '菌丝恢复稳定，进入出菇期环境精细调控。'
FROM greenhouse g
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM production_batch WHERE batch_no = 'ML-202606-A01')
LIMIT 1;

INSERT INTO production_batch_event(batch_id, event_code, event_title, event_status, operator, event_time, description, block_hash, previous_hash, sort_order)
SELECT b.id, 'SUBSTRATE_IN', '基质入棚', 'DONE', 'admin1', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 28 DAY), '完成培养基入棚、消毒记录和批次建档。', 'BATCH-A01-001', NULL, 10
FROM production_batch b WHERE b.batch_no = 'ML-202606-A01'
AND NOT EXISTS (SELECT 1 FROM production_batch_event WHERE batch_id = b.id AND event_code = 'SUBSTRATE_IN');

INSERT INTO production_batch_event(batch_id, event_code, event_title, event_status, operator, event_time, description, block_hash, previous_hash, sort_order)
SELECT b.id, 'MYCELIUM_RECOVERY', '菌丝恢复', 'DONE', 'admin2', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 14 DAY), '湿度目标上调至 86%，菌丝恢复状态良好。', 'BATCH-A01-002', 'BATCH-A01-001', 20
FROM production_batch b WHERE b.batch_no = 'ML-202606-A01'
AND NOT EXISTS (SELECT 1 FROM production_batch_event WHERE batch_id = b.id AND event_code = 'MYCELIUM_RECOVERY');

INSERT INTO production_batch_event(batch_id, event_code, event_title, event_status, operator, event_time, description, block_hash, previous_hash, sort_order)
SELECT b.id, 'FRUITING_CONTROL', '出菇期调控', 'RUNNING', 'admin1', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY), '温度、湿度、CO2 浓度进入连续监测与告警闭环。', 'BATCH-A01-003', 'BATCH-A01-002', 30
FROM production_batch b WHERE b.batch_no = 'ML-202606-A01'
AND NOT EXISTS (SELECT 1 FROM production_batch_event WHERE batch_id = b.id AND event_code = 'FRUITING_CONTROL');

INSERT INTO greenhouse(owner_user_id, name, location, status, area, crop_stage, created_by)
SELECT u.id, 'A02 羊肚菌育菇大棚', '温室二区 / 东侧', 'WARNING', 360.00, '菌丝恢复期', 'system'
FROM app_user u WHERE u.username = 'farmer001'
AND NOT EXISTS (SELECT 1 FROM greenhouse WHERE name = 'A02 羊肚菌育菇大棚' AND deleted = FALSE);

INSERT INTO greenhouse(owner_user_id, name, location, status, area, crop_stage, created_by)
SELECT u.id, 'A03 羊肚菌试验大棚', '温室二区 / 西侧', 'ONLINE', 300.00, '催菇期', 'system'
FROM app_user u WHERE u.username = 'farmer001'
AND NOT EXISTS (SELECT 1 FROM greenhouse WHERE name = 'A03 羊肚菌试验大棚' AND deleted = FALSE);

INSERT INTO farmer_greenhouse_binding(farmer_user_id, greenhouse_id)
SELECT u.id, g.id FROM app_user u, greenhouse g
WHERE u.username = 'farmer001' AND g.name IN ('A02 羊肚菌育菇大棚', 'A03 羊肚菌试验大棚') AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM farmer_greenhouse_binding b WHERE b.greenhouse_id = g.id AND b.deleted = FALSE);

INSERT INTO greenhouse_device(greenhouse_id, device_type_id, name, category, status, location, remark, manufacturer, model, protocol, auto_mode, health_score, created_by)
SELECT g.id, dt.id, '雾化加湿器', dt.category, 'RUNNING', '北侧雾化带', '根据湿度阈值自动补湿', 'Morel IoT Lab', 'HMD-MR-2026', dt.protocol, TRUE, 92, 'system'
FROM greenhouse g JOIN device_type dt ON dt.type_code = 'HUMIDIFIER'
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_device WHERE name = '雾化加湿器' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO greenhouse_device(greenhouse_id, device_type_id, name, category, status, location, remark, manufacturer, model, protocol, auto_mode, health_score, created_by)
SELECT g.id, dt.id, 'CO2 传感器', dt.category, 'RUNNING', '棚内中部', '每 5 分钟采集一次 CO2 浓度', 'Morel IoT Lab', 'CO2-MR-2026', dt.protocol, TRUE, 98, 'system'
FROM greenhouse g JOIN device_type dt ON dt.type_code = 'CO2_SENSOR'
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_device WHERE name = 'CO2 传感器' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO greenhouse_device(greenhouse_id, device_type_id, name, category, status, location, remark, manufacturer, model, protocol, auto_mode, health_score, created_by)
SELECT g.id, dt.id, '灌溉水泵', dt.category, 'MAINTENANCE', '西侧管线井', '昨日巡检发现压力波动，待复核', 'Morel IoT Lab', 'PUMP-MR-2026', dt.protocol, FALSE, 58, 'system'
FROM greenhouse g JOIN device_type dt ON dt.type_code = 'IRRIGATION_PUMP'
WHERE g.name = 'A02 羊肚菌育菇大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_device WHERE name = '灌溉水泵' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO greenhouse_device(greenhouse_id, device_type_id, name, category, status, location, remark, manufacturer, model, protocol, auto_mode, health_score, created_by)
SELECT g.id, dt.id, 'A03 循环风机', dt.category, 'STOPPED', '南侧风道', '催菇期低速通风备用', 'Morel IoT Lab', 'FAN-MR-2026', dt.protocol, TRUE, 87, 'system'
FROM greenhouse g JOIN device_type dt ON dt.type_code = 'VENTILATION_FAN'
WHERE g.name = 'A03 羊肚菌试验大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_device WHERE name = 'A03 循环风机' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO telemetry_snapshot(greenhouse_id, temperature, humidity, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, light_lux, co2_ppm, soil_moisture, collected_at)
SELECT g.id, v.temperature, v.humidity, v.temperature, v.humidity, v.soil_temperature, v.soil_moisture, v.ph_value, v.light_lux, v.co2_ppm, v.soil_moisture, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL v.hour_offset HOUR)
FROM greenhouse g
JOIN (
  SELECT 1 AS hour_offset, 21.5 AS temperature, 84.0 AS humidity, 20.3 AS soil_temperature, 4200 AS light_lux, 760 AS co2_ppm, 61.5 AS soil_moisture, 6.7 AS ph_value
  UNION ALL SELECT 2, 21.1, 85.2, 20.1, 4100, 790, 62.0, 6.8
  UNION ALL SELECT 3, 20.8, 86.4, 19.9, 3950, 830, 62.7, 6.7
  UNION ALL SELECT 4, 20.4, 87.1, 19.5, 3800, 870, 63.2, 6.6
  UNION ALL SELECT 5, 20.0, 86.8, 19.2, 3600, 910, 63.5, 6.6
  UNION ALL SELECT 6, 19.8, 85.9, 19.1, 3400, 880, 63.1, 6.7
) v ON TRUE
WHERE g.name = 'A01 羊肚菌智能大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM telemetry_snapshot ts WHERE ts.greenhouse_id = g.id AND ts.collected_at > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 HOUR));

INSERT INTO telemetry_snapshot(greenhouse_id, temperature, humidity, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, light_lux, co2_ppm, soil_moisture)
SELECT g.id, 18.9, 91.2, 18.9, 91.2, 18.2, 67.4, 6.5, 3100, 1180, 67.4 FROM greenhouse g
WHERE g.name = 'A02 羊肚菌育菇大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM telemetry_snapshot ts WHERE ts.greenhouse_id = g.id AND ts.temperature = 18.9);

INSERT INTO telemetry_snapshot(greenhouse_id, temperature, humidity, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, light_lux, co2_ppm, soil_moisture)
SELECT g.id, 22.4, 78.6, 22.4, 78.6, 21.1, 59.8, 6.9, 4450, 690, 59.8 FROM greenhouse g
WHERE g.name = 'A03 羊肚菌试验大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM telemetry_snapshot ts WHERE ts.greenhouse_id = g.id AND ts.temperature = 22.4);

INSERT INTO greenhouse_alert(greenhouse_id, device_id, rule_id, title, description, level, status, created_by)
SELECT g.id, d.id, r.id, 'CO2 浓度接近上限', 'A02 大棚 CO2 浓度持续升高，建议检查通风策略。', 'CRITICAL', 'OPEN', 'system'
FROM greenhouse g
LEFT JOIN greenhouse_device d ON d.greenhouse_id = g.id AND d.deleted = FALSE
LEFT JOIN alert_rule r ON r.rule_code = 'CO2_HIGH_A01'
WHERE g.name = 'A02 羊肚菌育菇大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_alert WHERE title = 'CO2 浓度接近上限' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO greenhouse_alert(greenhouse_id, device_id, rule_id, title, description, level, status, created_by)
SELECT g.id, d.id, r.id, '水泵压力波动', '灌溉水泵压力波动，已安排复核。', 'WARNING', 'ACKNOWLEDGED', 'system'
FROM greenhouse g
LEFT JOIN greenhouse_device d ON d.greenhouse_id = g.id AND d.name = '灌溉水泵' AND d.deleted = FALSE
LEFT JOIN alert_rule r ON r.rule_code = 'HUMIDITY_HIGH_A01'
WHERE g.name = 'A02 羊肚菌育菇大棚' AND g.deleted = FALSE
AND NOT EXISTS (SELECT 1 FROM greenhouse_alert WHERE title = '水泵压力波动' AND greenhouse_id = g.id AND deleted = FALSE);

INSERT INTO production_batch(greenhouse_id, batch_no, batch_name, crop_name, status, started_at, expected_harvest_at, summary)
SELECT g.id, 'ML-202606-A02', 'A02 菌丝恢复批次', '羊肚菌', 'RUNNING', DATE_SUB(CURRENT_DATE, INTERVAL 16 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 22 DAY), '菌丝恢复期湿度偏高，重点关注通风和水分控制。'
FROM greenhouse g
WHERE g.name = 'A02 羊肚菌育菇大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM production_batch WHERE batch_no = 'ML-202606-A02')
LIMIT 1;

INSERT INTO production_batch(greenhouse_id, batch_no, batch_name, crop_name, status, started_at, expected_harvest_at, summary)
SELECT g.id, 'ML-202606-A03', 'A03 催菇试验批次', '羊肚菌', 'RUNNING', DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), '催菇参数试验中，重点观察温差和光照变化。'
FROM greenhouse g
WHERE g.name = 'A03 羊肚菌试验大棚' AND g.deleted = FALSE
  AND NOT EXISTS (SELECT 1 FROM production_batch WHERE batch_no = 'ML-202606-A03')
LIMIT 1;

INSERT INTO feedback_conversation(farmer_user_id, admin_user_id, last_message, last_message_at)
SELECT f.id, a.id, 'A02 大棚 CO2 偏高，请协助查看通风策略。', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 MINUTE)
FROM app_user f, app_user a
WHERE f.username = 'farmer001' AND a.username = 'admin1'
  AND NOT EXISTS (
    SELECT 1 FROM feedback_conversation c
    WHERE c.farmer_user_id = f.id AND c.admin_user_id = a.id AND c.deleted = FALSE
  );

INSERT INTO feedback_message(conversation_id, sender_user_id, receiver_user_id, content, message_type)
SELECT c.id, c.farmer_user_id, c.admin_user_id, 'A02 大棚 CO2 偏高，请协助查看通风策略。', 'TEXT'
FROM feedback_conversation c
JOIN app_user f ON f.id = c.farmer_user_id
JOIN app_user a ON a.id = c.admin_user_id
WHERE f.username = 'farmer001' AND a.username = 'admin1'
  AND NOT EXISTS (
    SELECT 1 FROM feedback_message m
    WHERE m.conversation_id = c.id AND m.content = 'A02 大棚 CO2 偏高，请协助查看通风策略。'
  );
