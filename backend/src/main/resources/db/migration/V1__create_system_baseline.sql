CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  organization_code VARCHAR(64) NULL,
  status VARCHAR(20) NOT NULL,
  last_login_at TIMESTAMP(6) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_role_code UNIQUE (role_code)
);

CREATE TABLE sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
);

CREATE TABLE sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_id BIGINT NULL,
  menu_code VARCHAR(100) NOT NULL,
  label VARCHAR(100) NOT NULL,
  menu_type VARCHAR(20) NOT NULL,
  route VARCHAR(255) NULL,
  component_key VARCHAR(255) NULL,
  permission_code VARCHAR(160) NULL,
  sort_order INT NOT NULL,
  phase INT NOT NULL DEFAULT 0,
  visible BOOLEAN NOT NULL DEFAULT TRUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_menu_code UNIQUE (menu_code),
  CONSTRAINT uk_sys_menu_route UNIQUE (route),
  CONSTRAINT fk_sys_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu (id)
);

CREATE TABLE sys_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  permission_code VARCHAR(160) NOT NULL,
  domain_code VARCHAR(64) NOT NULL,
  resource_code VARCHAR(100) NOT NULL,
  action_code VARCHAR(32) NOT NULL,
  description VARCHAR(255) NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_permission_code UNIQUE (permission_code)
);

CREATE TABLE sys_role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission_id),
  CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
);

CREATE TABLE sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_role_menu UNIQUE (role_id, menu_id),
  CONSTRAINT fk_sys_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_sys_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id)
);

CREATE TABLE sys_refresh_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  access_jti CHAR(32) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  revoked_at TIMESTAMP(6) NULL,
  device_info VARCHAR(255) NULL,
  last_used_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_refresh_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_sys_refresh_token_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  INDEX idx_sys_refresh_token_active (user_id, revoked_at, expires_at)
);

CREATE TABLE sys_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL,
  success BOOLEAN NOT NULL,
  ip_digest CHAR(64) NULL,
  reason_code VARCHAR(64) NULL,
  trace_id VARCHAR(64) NOT NULL,
  occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_login_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  INDEX idx_sys_login_log_user_time (user_id, occurred_at)
);

CREATE TABLE sys_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  action_code VARCHAR(100) NOT NULL,
  target_type VARCHAR(100) NULL,
  target_id VARCHAR(100) NULL,
  result_code VARCHAR(64) NOT NULL,
  detail_summary VARCHAR(500) NULL,
  trace_id VARCHAR(64) NOT NULL,
  occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_operation_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  INDEX idx_sys_operation_log_user_time (user_id, occurred_at)
);

