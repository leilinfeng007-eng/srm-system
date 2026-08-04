-- SRM Stage 1: Master data tables and integration infrastructure

-- ============================================================
-- Purchasing organization (belongs to an md_organization)
-- ============================================================
CREATE TABLE md_purchasing_organization (
  id BIGINT NOT NULL AUTO_INCREMENT,
  po_code VARCHAR(64) NOT NULL,
  po_name VARCHAR(100) NOT NULL,
  company_org_id BIGINT NULL,
  responsible_user_id BIGINT NULL,
  default_currency VARCHAR(3) DEFAULT 'CNY',
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
  CONSTRAINT uk_md_po_code UNIQUE (po_code),
  CONSTRAINT uk_md_po_src_ext UNIQUE (source_system, external_id),
  CONSTRAINT fk_md_po_company_org FOREIGN KEY (company_org_id) REFERENCES md_organization (id),
  CONSTRAINT fk_md_po_resp_user FOREIGN KEY (responsible_user_id) REFERENCES sys_user (id)
);
CREATE INDEX idx_md_po_company_org ON md_purchasing_organization (company_org_id);
CREATE INDEX idx_md_po_status ON md_purchasing_organization (status);

-- ============================================================
-- Delivery location (belongs to a plant, optionally a warehouse)
-- ============================================================
CREATE TABLE md_delivery_location (
  id BIGINT NOT NULL AUTO_INCREMENT,
  location_code VARCHAR(64) NOT NULL,
  location_name VARCHAR(100) NOT NULL,
  plant_id BIGINT NULL,
  warehouse_id BIGINT NULL,
  address VARCHAR(500),
  contact_person VARCHAR(100),
  contact_phone VARCHAR(32),
  appointment_required BOOLEAN DEFAULT FALSE,
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
  CONSTRAINT uk_md_dl_loc_code UNIQUE (location_code),
  CONSTRAINT fk_md_dl_plant FOREIGN KEY (plant_id) REFERENCES md_plant (id),
  CONSTRAINT fk_md_dl_warehouse FOREIGN KEY (warehouse_id) REFERENCES md_warehouse (id)
);
CREATE INDEX idx_md_dl_plant ON md_delivery_location (plant_id);
CREATE INDEX idx_md_dl_warehouse ON md_delivery_location (warehouse_id);
CREATE INDEX idx_md_dl_status ON md_delivery_location (status);

-- ============================================================
-- Category (hierarchical, belongs to an organization, managed by a user)
-- ============================================================
CREATE TABLE md_category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(100) NOT NULL,
  parent_id BIGINT NULL,
  path VARCHAR(500),
  level INT NOT NULL DEFAULT 0,
  responsible_org_id BIGINT NULL,
  category_manager_id BIGINT NULL,
  risk_level VARCHAR(10) DEFAULT 'LOW',
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
  CONSTRAINT uk_md_cat_code UNIQUE (category_code),
  CONSTRAINT fk_md_cat_parent FOREIGN KEY (parent_id) REFERENCES md_category (id),
  CONSTRAINT fk_md_cat_resp_org FOREIGN KEY (responsible_org_id) REFERENCES md_organization (id),
  CONSTRAINT fk_md_cat_manager FOREIGN KEY (category_manager_id) REFERENCES sys_user (id)
);
CREATE INDEX idx_md_cat_parent ON md_category (parent_id);
CREATE INDEX idx_md_cat_path ON md_category (path);
CREATE INDEX idx_md_cat_status ON md_category (status);

-- ============================================================
-- Unit of measure
-- ============================================================
CREATE TABLE md_unit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  unit_code VARCHAR(32) NOT NULL,
  unit_name VARCHAR(50) NOT NULL,
  dimension VARCHAR(50) NULL,
  decimal_places INT DEFAULT 0,
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
  CONSTRAINT uk_md_unit_code UNIQUE (unit_code)
);

-- ============================================================
-- Currency
-- ============================================================
CREATE TABLE md_currency (
  id BIGINT NOT NULL AUTO_INCREMENT,
  currency_code VARCHAR(3) NOT NULL,
  currency_name VARCHAR(50) NOT NULL,
  symbol VARCHAR(10),
  decimal_places INT DEFAULT 2,
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
  CONSTRAINT uk_md_curr_code UNIQUE (currency_code)
);

-- ============================================================
-- Tax code
-- ============================================================
CREATE TABLE md_tax_code (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tax_code VARCHAR(32) NOT NULL,
  tax_name VARCHAR(100) NOT NULL,
  country VARCHAR(100) NOT NULL,
  tax_rate DECIMAL(10,4) NOT NULL,
  effective_from TIMESTAMP(6) NULL,
  effective_to TIMESTAMP(6) NULL,
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
  CONSTRAINT uk_md_tax_code UNIQUE (tax_code)
);

-- ============================================================
-- Material (belongs to a category)
-- ============================================================
CREATE TABLE md_material (
  id BIGINT NOT NULL AUTO_INCREMENT,
  material_code VARCHAR(64) NOT NULL,
  material_name VARCHAR(200) NOT NULL,
  specification VARCHAR(500),
  material_type VARCHAR(32) NULL,
  base_unit VARCHAR(32) NULL,
  category_id BIGINT NULL,
  material_version VARCHAR(20) DEFAULT '1',
  is_critical BOOLEAN DEFAULT FALSE,
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
  CONSTRAINT uk_md_mat_code UNIQUE (material_code),
  CONSTRAINT fk_md_mat_cat FOREIGN KEY (category_id) REFERENCES md_category (id)
);
CREATE INDEX idx_md_mat_category ON md_material (category_id);
CREATE INDEX idx_md_mat_status ON md_material (status);
CREATE INDEX idx_md_mat_src_sys ON md_material (source_system);

-- ============================================================
-- External system mapping (correlates internal IDs with external system IDs)
-- ============================================================
CREATE TABLE md_external_mapping (
  id BIGINT NOT NULL AUTO_INCREMENT,
  object_type VARCHAR(64) NOT NULL,
  internal_id VARCHAR(100) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  external_id VARCHAR(200) NOT NULL,
  external_line_id VARCHAR(200) NULL,
  external_version BIGINT NULL DEFAULT 0,
  mapping_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  conflict_summary VARCHAR(500) NULL,
  last_sync_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_md_map_src_obj_ext UNIQUE (source_system, object_type, external_id)
);
CREATE INDEX idx_md_map_obj_int ON md_external_mapping (object_type, internal_id);
CREATE INDEX idx_md_map_src_sys ON md_external_mapping (source_system);

-- ============================================================
-- Outbox event (events to be published to external systems)
-- ============================================================
CREATE TABLE sys_outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(128) NOT NULL,
  object_type VARCHAR(64) NOT NULL,
  object_id VARCHAR(100) NOT NULL,
  object_version BIGINT NOT NULL DEFAULT 0,
  event_type VARCHAR(64) NOT NULL,
  payload_summary VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  attempt_count INT DEFAULT 0,
  max_attempts INT DEFAULT 5,
  next_retry_at TIMESTAMP(6) NULL,
  last_error VARCHAR(500) NULL,
  trace_id VARCHAR(64),
  occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX idx_sys_outbox_status ON sys_outbox_event (status);
CREATE INDEX idx_sys_outbox_obj_type ON sys_outbox_event (object_type);
CREATE INDEX idx_sys_outbox_retry ON sys_outbox_event (next_retry_at);

-- ============================================================
-- Inbox event (events received from external systems)
-- ============================================================
CREATE TABLE sys_inbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(128) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  object_type VARCHAR(64) NOT NULL,
  object_id VARCHAR(100) NOT NULL,
  object_version BIGINT NOT NULL DEFAULT 0,
  payload_summary VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
  result_message VARCHAR(500) NULL,
  received_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  processed_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_inbox_event_id UNIQUE (event_id)
);
CREATE INDEX idx_sys_inbox_src_obj ON sys_inbox_event (source_system, object_type, object_id);
CREATE INDEX idx_sys_inbox_status ON sys_inbox_event (status);

-- ============================================================
-- Integration job (tracks batch/sync/outbox/inbox jobs)
-- ============================================================
CREATE TABLE sys_integration_job (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_type VARCHAR(32) NOT NULL,
  object_type VARCHAR(64) NOT NULL,
  source_id VARCHAR(200) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id)
);
CREATE INDEX idx_sys_ij_job_type ON sys_integration_job (job_type);
CREATE INDEX idx_sys_ij_status ON sys_integration_job (status);

-- ============================================================
-- Integration attempt (per-attempt record for an integration job)
-- ============================================================
CREATE TABLE sys_integration_attempt (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  attempt_number INT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL,
  error_message VARCHAR(500) NULL,
  trace_id VARCHAR(64),
  attempted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_ia_job FOREIGN KEY (job_id) REFERENCES sys_integration_job (id)
);
CREATE INDEX idx_sys_ia_job_id ON sys_integration_attempt (job_id);
