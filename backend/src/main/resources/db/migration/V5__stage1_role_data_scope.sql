-- SRM Stage 1: Role data policy, permission phase, role-menu/role-permission version

-- ============================================================
-- Role data scope policy (brand new table)
-- ============================================================
CREATE TABLE sys_role_data_policy (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  domain_code VARCHAR(64) NOT NULL,
  dimension_code VARCHAR(32) NOT NULL,
  scope_type VARCHAR(10) NOT NULL,
  include_children BOOLEAN NOT NULL DEFAULT FALSE,
  operation_mode VARCHAR(16) NOT NULL DEFAULT 'READ_WRITE',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_role_data_policy UNIQUE (role_id, domain_code, dimension_code),
  CONSTRAINT fk_sys_rdp_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  INDEX idx_sys_rdp_role_id (role_id),
  INDEX idx_sys_rdp_domain (domain_code),
  INDEX idx_sys_rdp_dimension (dimension_code)
);

-- ============================================================
-- ALTER sys_permission: add phase to match stage1 permission lifecycle
-- ============================================================
ALTER TABLE sys_permission ADD COLUMN phase INT NOT NULL DEFAULT 1;

-- ============================================================
-- ALTER sys_role_menu: add version for optimistic locking
-- ============================================================
ALTER TABLE sys_role_menu ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ============================================================
-- ALTER sys_role_permission: add version for optimistic locking
-- ============================================================
ALTER TABLE sys_role_permission ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
