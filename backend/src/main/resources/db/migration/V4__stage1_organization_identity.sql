-- SRM Stage 1: Organization, Department, Position, User extensions, Role association history

-- ============================================================
-- Organization tree (GROUP -> COMPANY -> BUSINESS_UNIT -> PLANT -> WAREHOUSE)
-- ============================================================
CREATE TABLE md_organization (
  id BIGINT NOT NULL AUTO_INCREMENT,
  org_code VARCHAR(64) NOT NULL,
  org_name VARCHAR(100) NOT NULL,
  org_type VARCHAR(20) NOT NULL,
  parent_id BIGINT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  path VARCHAR(500) NULL,
  level INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  description VARCHAR(255) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_md_org_code UNIQUE (org_code),
  CONSTRAINT fk_md_org_parent FOREIGN KEY (parent_id) REFERENCES md_organization (id),
  INDEX idx_md_org_parent (parent_id),
  INDEX idx_md_org_path (path),
  INDEX idx_md_org_type_status (org_type, status)
);

-- ============================================================
-- Plant extension (one-to-one with PLANT-type organization node)
-- ============================================================
CREATE TABLE md_plant (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  plant_code VARCHAR(64) NOT NULL,
  plant_name VARCHAR(100) NOT NULL,
  timezone VARCHAR(50) DEFAULT 'UTC',
  country VARCHAR(100) NULL,
  address VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  source_system VARCHAR(64) NOT NULL DEFAULT 'SRM',
  external_id VARCHAR(100) NULL,
  external_version BIGINT NULL DEFAULT 0,
  last_sync_at TIMESTAMP(6) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_md_plant_code UNIQUE (plant_code),
  CONSTRAINT uk_md_plant_org UNIQUE (organization_id),
  CONSTRAINT fk_md_plant_org FOREIGN KEY (organization_id) REFERENCES md_organization (id),
  INDEX idx_md_plant_org (organization_id)
);

-- ============================================================
-- Warehouse extension (one-to-one with WAREHOUSE-type organization node)
-- ============================================================
CREATE TABLE md_warehouse (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  warehouse_code VARCHAR(64) NOT NULL,
  warehouse_name VARCHAR(100) NOT NULL,
  plant_id BIGINT NOT NULL,
  warehouse_type VARCHAR(32) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  source_system VARCHAR(64) NOT NULL DEFAULT 'SRM',
  external_id VARCHAR(100) NULL,
  external_version BIGINT NULL DEFAULT 0,
  last_sync_at TIMESTAMP(6) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_md_warehouse_code UNIQUE (warehouse_code),
  CONSTRAINT uk_md_warehouse_org UNIQUE (organization_id),
  CONSTRAINT fk_md_warehouse_org FOREIGN KEY (organization_id) REFERENCES md_organization (id),
  CONSTRAINT fk_md_warehouse_plant FOREIGN KEY (plant_id) REFERENCES md_plant (id),
  INDEX idx_md_warehouse_org (organization_id),
  INDEX idx_md_warehouse_plant (plant_id)
);

-- ============================================================
-- Department (belongs to an organization)
-- ============================================================
CREATE TABLE sys_department (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dept_code VARCHAR(64) NOT NULL,
  dept_name VARCHAR(100) NOT NULL,
  organization_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  manager_name VARCHAR(100) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  path VARCHAR(500) NULL,
  level INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  description VARCHAR(255) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_dept_code UNIQUE (dept_code),
  CONSTRAINT fk_sys_dept_org FOREIGN KEY (organization_id) REFERENCES md_organization (id),
  CONSTRAINT fk_sys_dept_parent FOREIGN KEY (parent_id) REFERENCES sys_department (id),
  INDEX idx_sys_dept_org (organization_id),
  INDEX idx_sys_dept_parent (parent_id)
);

-- ============================================================
-- Position (belongs to a department)
-- ============================================================
CREATE TABLE sys_position (
  id BIGINT NOT NULL AUTO_INCREMENT,
  position_code VARCHAR(64) NOT NULL,
  position_name VARCHAR(100) NOT NULL,
  department_id BIGINT NOT NULL,
  responsibility VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_position_code UNIQUE (position_code),
  CONSTRAINT fk_sys_position_dept FOREIGN KEY (department_id) REFERENCES sys_department (id),
  INDEX idx_sys_position_dept (department_id)
);

-- ============================================================
-- ALTER sys_user: add main organization/department/position, employee code, email, phone, password change flag
-- ============================================================
ALTER TABLE sys_user ADD COLUMN main_organization_id BIGINT NULL;
ALTER TABLE sys_user ADD COLUMN main_department_id BIGINT NULL;
ALTER TABLE sys_user ADD COLUMN main_position_id BIGINT NULL;
ALTER TABLE sys_user ADD COLUMN employee_code VARCHAR(64) NULL;
ALTER TABLE sys_user ADD COLUMN email VARCHAR(128) NULL;
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(32) NULL;
ALTER TABLE sys_user ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_user ADD CONSTRAINT uk_sys_user_employee_code UNIQUE (employee_code);
ALTER TABLE sys_user ADD CONSTRAINT fk_sys_user_org FOREIGN KEY (main_organization_id) REFERENCES md_organization (id);
ALTER TABLE sys_user ADD CONSTRAINT fk_sys_user_dept FOREIGN KEY (main_department_id) REFERENCES sys_department (id);
ALTER TABLE sys_user ADD CONSTRAINT fk_sys_user_position FOREIGN KEY (main_position_id) REFERENCES sys_position (id);
CREATE INDEX idx_sys_user_org ON sys_user (main_organization_id);
CREATE INDEX idx_sys_user_dept ON sys_user (main_department_id);

-- ============================================================
-- ALTER sys_user_role: add status, version, effective_from/to for stateful association
-- ============================================================
ALTER TABLE sys_user_role ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE sys_user_role ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_role ADD COLUMN effective_from TIMESTAMP(6) NULL;
ALTER TABLE sys_user_role ADD COLUMN effective_to TIMESTAMP(6) NULL;
CREATE INDEX idx_sys_user_role_status ON sys_user_role (status);
CREATE INDEX idx_sys_user_role_effective ON sys_user_role (effective_from, effective_to);

-- ============================================================
-- User assignment history (organization/department/position changes)
-- ============================================================
CREATE TABLE sys_user_assignment_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  field_name VARCHAR(64) NOT NULL,
  old_value VARCHAR(255) NULL,
  new_value VARCHAR(255) NOT NULL,
  change_reason VARCHAR(255) NULL,
  changed_by VARCHAR(64) NOT NULL,
  changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_uah_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  INDEX idx_sys_uah_user (user_id),
  INDEX idx_sys_uah_time (changed_at)
);

-- ============================================================
-- User-role association history
-- ============================================================
CREATE TABLE sys_user_role_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,
  previous_status VARCHAR(20) NULL,
  new_status VARCHAR(20) NOT NULL,
  changed_by VARCHAR(64) NOT NULL,
  changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_urh_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_urh_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  INDEX idx_sys_urh_user (user_id),
  INDEX idx_sys_urh_role (role_id),
  INDEX idx_sys_urh_time (changed_at)
);

-- ============================================================
-- ALTER sys_role: add built_in flag, version
-- ============================================================
ALTER TABLE sys_role ADD COLUMN built_in BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_role ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_sys_role_built_in ON sys_role (built_in);

-- Mark existing bootstrap roles as built_in
UPDATE sys_role SET built_in = TRUE WHERE id IN (100, 101);
