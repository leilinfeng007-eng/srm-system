-- SRM Stage 1: Dictionary, parameter, numbering, attachment, template, batch job, operation log extensions

-- ============================================================
-- Dictionary header
-- ============================================================
CREATE TABLE sys_dictionary (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_code VARCHAR(64) NOT NULL,
  dict_name VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  description VARCHAR(255) NULL,
  effective_from TIMESTAMP(6) NULL,
  effective_to TIMESTAMP(6) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_dict_code UNIQUE (dict_code)
);

-- ============================================================
-- Dictionary item (entries under a dictionary)
-- ============================================================
CREATE TABLE sys_dictionary_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_id BIGINT NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  description VARCHAR(255) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_dict_item_dict_code UNIQUE (dict_id, item_code),
  CONSTRAINT fk_sys_dict_item_dict FOREIGN KEY (dict_id) REFERENCES sys_dictionary (id)
);
CREATE INDEX idx_sys_dict_item_dict_id ON sys_dictionary_item (dict_id);

-- ============================================================
-- System parameter header
-- ============================================================
CREATE TABLE sys_parameter (
  id BIGINT NOT NULL AUTO_INCREMENT,
  param_code VARCHAR(64) NOT NULL,
  param_name VARCHAR(100) NOT NULL,
  param_type VARCHAR(20) NOT NULL,
  default_value VARCHAR(500) NULL,
  validation_rule VARCHAR(500) NULL,
  approval_required BOOLEAN NOT NULL DEFAULT FALSE,
  description VARCHAR(255) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_param_code UNIQUE (param_code)
);

-- ============================================================
-- Parameter version (auditable value history with approval lifecycle)
-- ============================================================
CREATE TABLE sys_parameter_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  param_id BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  param_value VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  effective_from TIMESTAMP(6) NULL,
  published_by VARCHAR(64) NULL,
  published_at TIMESTAMP(6) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_param_ver_pid_ver UNIQUE (param_id, version),
  CONSTRAINT fk_sys_param_ver_param FOREIGN KEY (param_id) REFERENCES sys_parameter (id)
);
CREATE INDEX idx_sys_param_ver_param_id ON sys_parameter_version (param_id);
CREATE INDEX idx_sys_param_ver_status ON sys_parameter_version (status);

-- ============================================================
-- Numbering rule definition
-- ============================================================
CREATE TABLE sys_number_rule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(100) NOT NULL,
  object_type VARCHAR(64) NOT NULL,
  prefix VARCHAR(20) NOT NULL,
  date_format VARCHAR(20) NULL,
  serial_length INT NOT NULL DEFAULT 5,
  reset_cycle VARCHAR(10) NOT NULL DEFAULT 'NONE',
  organization_dimension BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_num_rule_code UNIQUE (rule_code)
);

-- ============================================================
-- Numbering sequence (current counter per rule/org/period)
-- ============================================================
CREATE TABLE sys_number_sequence (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rule_id BIGINT NOT NULL,
  org_id BIGINT NULL,
  period_key VARCHAR(20) NOT NULL,
  current_sequence BIGINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_num_seq_rule_org_period UNIQUE (rule_id, org_id, period_key),
  CONSTRAINT fk_sys_num_seq_rule FOREIGN KEY (rule_id) REFERENCES sys_number_rule (id)
);
CREATE INDEX idx_sys_num_seq_rule_id ON sys_number_sequence (rule_id);

-- ============================================================
-- Attachment metadata
-- ============================================================
CREATE TABLE sys_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  file_size BIGINT NOT NULL,
  file_sha256 CHAR(64) NOT NULL,
  owner_type VARCHAR(64) NOT NULL,
  owner_id VARCHAR(100) NOT NULL,
  scan_status VARCHAR(20) NOT NULL DEFAULT 'NOT_CONFIGURED',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_attach_storage_key UNIQUE (storage_key)
);
CREATE INDEX idx_sys_attach_owner ON sys_attachment (owner_type, owner_id);
CREATE INDEX idx_sys_attach_sha256 ON sys_attachment (file_sha256);

-- ============================================================
-- Attachment version history (immutable file revision records)
-- ============================================================
CREATE TABLE sys_attachment_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  attachment_id BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  file_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  file_size BIGINT NOT NULL,
  file_sha256 CHAR(64) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_attach_ver_aid_ver UNIQUE (attachment_id, version),
  CONSTRAINT uk_sys_attach_ver_storage_key UNIQUE (storage_key),
  CONSTRAINT fk_sys_attach_ver_attach FOREIGN KEY (attachment_id) REFERENCES sys_attachment (id)
);
CREATE INDEX idx_sys_attach_ver_attach_id ON sys_attachment_version (attachment_id);

-- ============================================================
-- Document template
-- ============================================================
CREATE TABLE sys_document_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  purpose VARCHAR(255) NULL,
  domain_code VARCHAR(64) NOT NULL,
  template_version INT NOT NULL DEFAULT 1,
  attachment_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_doc_tmpl_code_ver UNIQUE (template_code, template_version),
  CONSTRAINT fk_sys_doc_tmpl_attach FOREIGN KEY (attachment_id) REFERENCES sys_attachment (id)
);

-- ============================================================
-- Batch job
-- ============================================================
CREATE TABLE sys_batch_job (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_type VARCHAR(32) NOT NULL,
  object_type VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  total_count INT DEFAULT 0,
  success_count INT DEFAULT 0,
  fail_count INT DEFAULT 0,
  progress_percent INT DEFAULT 0,
  attachment_id BIGINT NULL,
  result_attachment_id BIGINT NULL,
  idempotency_key VARCHAR(128) NULL,
  started_at TIMESTAMP(6) NULL,
  completed_at TIMESTAMP(6) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_batch_job_idem UNIQUE (idempotency_key),
  CONSTRAINT fk_sys_batch_job_attach FOREIGN KEY (attachment_id) REFERENCES sys_attachment (id),
  CONSTRAINT fk_sys_batch_job_result_attach FOREIGN KEY (result_attachment_id) REFERENCES sys_attachment (id)
);
CREATE INDEX idx_sys_batch_job_obj_type ON sys_batch_job (object_type);
CREATE INDEX idx_sys_batch_job_status ON sys_batch_job (status);

-- ============================================================
-- Batch job error detail
-- ============================================================
CREATE TABLE sys_batch_job_error (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  error_row INT NULL,
  field_name VARCHAR(64) NULL,
  error_code VARCHAR(64) NOT NULL,
  error_message VARCHAR(500) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_batch_job_err_job FOREIGN KEY (job_id) REFERENCES sys_batch_job (id)
);
CREATE INDEX idx_sys_batch_job_err_job_id ON sys_batch_job_error (job_id);

-- ============================================================
-- Extend sys_operation_log with audit fields
-- ============================================================
ALTER TABLE sys_operation_log ADD COLUMN field_changes TEXT NULL;
ALTER TABLE sys_operation_log ADD COLUMN before_hash VARCHAR(128) NULL;
ALTER TABLE sys_operation_log ADD COLUMN after_hash VARCHAR(128) NULL;
ALTER TABLE sys_operation_log ADD COLUMN reason VARCHAR(255) NULL;
ALTER TABLE sys_operation_log ADD COLUMN operator_name VARCHAR(100) NULL;
