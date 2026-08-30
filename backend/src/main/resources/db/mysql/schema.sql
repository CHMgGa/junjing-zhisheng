CREATE TABLE IF NOT EXISTS sys_dict_type (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type_code VARCHAR(64) NOT NULL UNIQUE,
  type_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type_code VARCHAR(64) NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  item_value VARCHAR(255),
  tag_type VARCHAR(32),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  remark VARCHAR(500),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  UNIQUE(type_code, item_code)
);

CREATE TABLE IF NOT EXISTS auth_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(32) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  description VARCHAR(500),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF',
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_permission (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  permission_code VARCHAR(128) NOT NULL UNIQUE,
  permission_name VARCHAR(128) NOT NULL,
  resource_type VARCHAR(32) NOT NULL DEFAULT 'API',
  resource_path VARCHAR(255),
  http_method VARCHAR(16),
  parent_id BIGINT,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL REFERENCES auth_role(id),
  permission_id BIGINT NOT NULL REFERENCES auth_permission(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(128) NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  phone VARCHAR(32) UNIQUE,
  email VARCHAR(128) UNIQUE,
  display_name VARCHAR(64),
  avatar_url VARCHAR(512),
  gender VARCHAR(16),
  bio VARCHAR(500),
  allow_admin_delete BOOLEAN NOT NULL DEFAULT FALSE,
  last_login_ip VARCHAR(64),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  deleted_by VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id),
  role_id BIGINT NOT NULL REFERENCES auth_role(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, role_id)
);

CREATE TABLE IF NOT EXISTS greenhouse (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  owner_user_id BIGINT REFERENCES app_user(id),
  name VARCHAR(128) NOT NULL,
  location VARCHAR(255),
  status VARCHAR(32) NOT NULL,
  area NUMERIC(10,2) NOT NULL DEFAULT 0,
  crop_stage VARCHAR(64),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  deleted_by VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS farmer_greenhouse_binding (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  farmer_user_id BIGINT NOT NULL REFERENCES app_user(id),
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  assigned_by BIGINT REFERENCES app_user(id),
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMP,
  UNIQUE(greenhouse_id)
);

CREATE TABLE IF NOT EXISTS device_type (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type_code VARCHAR(64) NOT NULL UNIQUE,
  type_name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  protocol VARCHAR(32) NOT NULL DEFAULT 'MODBUS',
  unit VARCHAR(32),
  telemetry_key VARCHAR(64),
  command_capable BOOLEAN NOT NULL DEFAULT TRUE,
  description VARCHAR(500),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS greenhouse_device (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  device_type_id BIGINT REFERENCES device_type(id),
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  location VARCHAR(255),
  remark VARCHAR(500),
  manufacturer VARCHAR(128),
  model VARCHAR(128),
  serial_no VARCHAR(128),
  protocol VARCHAR(32) DEFAULT 'MODBUS',
  auto_mode BOOLEAN NOT NULL DEFAULT FALSE,
  health_score INT NOT NULL DEFAULT 100,
  last_command VARCHAR(64),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  deleted_by VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS telemetry_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  temperature NUMERIC(6,2) NOT NULL,
  humidity NUMERIC(6,2) NOT NULL,
  air_temperature NUMERIC(6,2) NOT NULL DEFAULT 0,
  air_humidity NUMERIC(6,2) NOT NULL DEFAULT 0,
  soil_temperature NUMERIC(6,2) NOT NULL DEFAULT 0,
  soil_humidity NUMERIC(6,2) NOT NULL DEFAULT 0,
  ph_value NUMERIC(4,2) NOT NULL DEFAULT 6.80,
  light_lux INT NOT NULL,
  co2_ppm INT NOT NULL,
  soil_moisture NUMERIC(6,2) NOT NULL,
  collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  rule_code VARCHAR(64) NOT NULL UNIQUE,
  rule_name VARCHAR(128) NOT NULL,
  greenhouse_id BIGINT REFERENCES greenhouse(id),
  device_type_id BIGINT REFERENCES device_type(id),
  metric_key VARCHAR(64) NOT NULL,
  operator VARCHAR(16) NOT NULL,
  threshold_value NUMERIC(12,2) NOT NULL,
  duration_minutes INT NOT NULL DEFAULT 5,
  level VARCHAR(32) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  description VARCHAR(500),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  deleted_by VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS greenhouse_alert (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  device_id BIGINT REFERENCES greenhouse_device(id),
  rule_id BIGINT REFERENCES alert_rule(id),
  title VARCHAR(160) NOT NULL,
  description VARCHAR(500),
  level VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  handled_by VARCHAR(64),
  handle_note VARCHAR(500),
  handled_at TIMESTAMP,
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  deleted_by VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS traceability_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  batch_no VARCHAR(64) NOT NULL,
  operation VARCHAR(128) NOT NULL,
  operator VARCHAR(64) NOT NULL,
  operation_date DATE NOT NULL,
  note VARCHAR(500),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  deleted_by VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS production_batch (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_no VARCHAR(64) NOT NULL UNIQUE,
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  batch_name VARCHAR(128) NOT NULL,
  crop_name VARCHAR(64) NOT NULL DEFAULT '羊肚菌',
  status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
  started_at DATE NOT NULL,
  expected_harvest_at DATE,
  actual_harvest_at DATE,
  summary VARCHAR(500),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS production_batch_event (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES production_batch(id),
  event_code VARCHAR(64) NOT NULL,
  event_title VARCHAR(128) NOT NULL,
  event_status VARCHAR(32) NOT NULL DEFAULT 'DONE',
  operator VARCHAR(64),
  event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  description VARCHAR(500),
  block_hash VARCHAR(128),
  previous_hash VARCHAR(128),
  sort_order INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS verification_code (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  receiver VARCHAR(128) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  code VARCHAR(12) NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  client_ip VARCHAR(64),
  delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  delivery_message VARCHAR(500),
  sent_at TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS verification_send_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  receiver VARCHAR(128) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  channel VARCHAR(32) NOT NULL DEFAULT 'EMAIL',
  client_ip VARCHAR(64),
  delivery_mode VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  provider_message VARCHAR(500),
  error_message VARCHAR(1000),
  request_trace_id VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_session (
  token VARCHAR(64) PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id),
  username VARCHAR(64) NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  client_ip VARCHAR(64),
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT REFERENCES app_user(id),
  category VARCHAR(64) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  contact VARCHAR(128),
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS feedback_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  farmer_user_id BIGINT NOT NULL REFERENCES app_user(id),
  admin_user_id BIGINT NOT NULL REFERENCES app_user(id),
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  last_message VARCHAR(500),
  last_message_at TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  UNIQUE(farmer_user_id, admin_user_id)
);

CREATE TABLE IF NOT EXISTS feedback_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL REFERENCES feedback_conversation(id),
  sender_user_id BIGINT NOT NULL REFERENCES app_user(id),
  receiver_user_id BIGINT NOT NULL REFERENCES app_user(id),
  content VARCHAR(1000) NOT NULL,
  message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
  image_url VARCHAR(512),
  read_at TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_action_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  alert_id BIGINT NOT NULL REFERENCES greenhouse_alert(id),
  device_id BIGINT REFERENCES greenhouse_device(id),
  command VARCHAR(64),
  notify_user_id BIGINT REFERENCES app_user(id),
  operator VARCHAR(64),
  action_note VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  trace_id VARCHAR(64),
  user_id BIGINT,
  username VARCHAR(64),
  client_ip VARCHAR(64),
  module_name VARCHAR(64),
  action_name VARCHAR(128),
  request_uri VARCHAR(255),
  http_method VARCHAR(16),
  success BOOLEAN NOT NULL,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
  elapsed_ms BIGINT,
  message VARCHAR(500),
  request_digest VARCHAR(128),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id),
  greenhouse_id BIGINT REFERENCES greenhouse(id),
  title VARCHAR(160),
  conversation_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL REFERENCES ai_conversation(id),
  sender_type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  image_url TEXT,
  risk_level VARCHAR(32),
  diagnosis TEXT,
  references_json TEXT,
  expert_trace_json TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_suggestion (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  farmer_user_id BIGINT REFERENCES app_user(id),
  greenhouse_id BIGINT REFERENCES greenhouse(id),
  conversation_id BIGINT REFERENCES ai_conversation(id),
  title VARCHAR(160) NOT NULL,
  content TEXT NOT NULL,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  snapshot_id BIGINT,
  downlinked_by BIGINT REFERENCES app_user(id),
  downlinked_at TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL UNIQUE,
  file_path VARCHAR(1000),
  chunk_count INT NOT NULL DEFAULT 0,
  indexed_at TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS greenhouse_camera_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  greenhouse_id BIGINT NOT NULL REFERENCES greenhouse(id),
  device_id BIGINT REFERENCES greenhouse_device(id),
  image_url TEXT,
  image_base64 TEXT,
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  ai_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  ai_result_json TEXT,
  ai_error VARCHAR(1000),
  captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  analyzed_at TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP
);

CREATE INDEX idx_dict_item_type_enabled ON sys_dict_item(type_code, enabled, deleted);
CREATE INDEX idx_role_permission_role ON auth_role_permission(role_id);
CREATE INDEX idx_user_role_user ON auth_user_role(user_id);
CREATE INDEX idx_device_type_category ON device_type(category, enabled, deleted);
CREATE INDEX idx_greenhouse_owner_status ON greenhouse(owner_user_id, status, deleted);
CREATE INDEX idx_farmer_greenhouse_farmer ON farmer_greenhouse_binding(farmer_user_id, deleted);
CREATE INDEX idx_farmer_greenhouse_greenhouse ON farmer_greenhouse_binding(greenhouse_id, deleted);
CREATE INDEX idx_greenhouse_device_greenhouse_status ON greenhouse_device(greenhouse_id, status, deleted);
CREATE INDEX idx_greenhouse_device_type ON greenhouse_device(device_type_id);
CREATE INDEX idx_telemetry_greenhouse_time ON telemetry_snapshot(greenhouse_id, collected_at DESC);
CREATE INDEX idx_alert_rule_metric_enabled ON alert_rule(metric_key, enabled, deleted);
CREATE INDEX idx_alert_greenhouse_status_time ON greenhouse_alert(greenhouse_id, status, occurred_at DESC, deleted);
CREATE INDEX idx_alert_status_level ON greenhouse_alert(status, level);
CREATE INDEX idx_alert_rule ON greenhouse_alert(rule_id);
CREATE INDEX idx_feedback_status_time ON feedback(status, created_at DESC, deleted);
CREATE INDEX idx_batch_greenhouse_status_time ON production_batch(greenhouse_id, status, started_at DESC, deleted);
CREATE INDEX idx_batch_no ON production_batch(batch_no);
CREATE INDEX idx_batch_event_batch_order ON production_batch_event(batch_id, sort_order, event_time);
CREATE INDEX idx_feedback_conversation_farmer ON feedback_conversation(farmer_user_id, deleted, last_message_at DESC);
CREATE INDEX idx_feedback_conversation_admin ON feedback_conversation(admin_user_id, deleted, last_message_at DESC);
CREATE INDEX idx_feedback_message_conversation ON feedback_message(conversation_id, created_at);
CREATE INDEX idx_alert_action_alert ON alert_action_log(alert_id, created_at DESC);
CREATE INDEX idx_verification_code_receiver_scene_time ON verification_code(receiver, scene, created_at DESC);
CREATE INDEX idx_verification_code_expires_used ON verification_code(expires_at, used);
CREATE INDEX idx_verification_send_receiver_time ON verification_send_log(receiver, created_at DESC);
CREATE INDEX idx_verification_send_ip_time ON verification_send_log(client_ip, created_at DESC);
CREATE INDEX idx_operation_log_created_at ON operation_log(created_at DESC);
CREATE INDEX idx_operation_log_user_time ON operation_log(username, created_at DESC);
CREATE INDEX idx_operation_log_module_success_time ON operation_log(module_name, success, created_at DESC);
CREATE INDEX idx_operation_log_trace ON operation_log(trace_id);
CREATE INDEX idx_app_session_user_expires ON app_session(user_id, expires_at DESC);
CREATE INDEX idx_ai_conversation_user_time ON ai_conversation(user_id, created_at DESC, deleted);
CREATE INDEX idx_ai_message_conversation_time ON ai_message(conversation_id, created_at);
CREATE INDEX idx_ai_suggestion_status_time ON ai_suggestion(status, created_at DESC, deleted);
CREATE INDEX idx_ai_suggestion_farmer_time ON ai_suggestion(farmer_user_id, created_at DESC, deleted);
CREATE INDEX idx_ai_suggestion_source_time ON ai_suggestion(source_type, status, created_at DESC, deleted);
CREATE INDEX idx_camera_snapshot_ai_status ON greenhouse_camera_snapshot(ai_status, captured_at, deleted);
CREATE INDEX idx_camera_snapshot_greenhouse_time ON greenhouse_camera_snapshot(greenhouse_id, captured_at DESC, deleted);

CREATE OR REPLACE VIEW v_greenhouse_realtime AS
SELECT g.id, g.name, g.status,
       t.air_temperature, t.air_humidity, t.soil_temperature, t.soil_humidity,
       t.ph_value, t.light_lux, t.co2_ppm, t.collected_at
FROM greenhouse g
LEFT JOIN telemetry_snapshot t ON t.id = (
  SELECT ts.id FROM telemetry_snapshot ts
  WHERE ts.greenhouse_id = g.id
  ORDER BY ts.collected_at DESC
  LIMIT 1
)
WHERE g.deleted = FALSE;

