-- SRM Stage 1: Workflow definition, approval instances, unified tasks, in-site messages

-- ============================================================
-- Workflow process definition
-- ============================================================
CREATE TABLE sys_workflow_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  process_code VARCHAR(64) NOT NULL,
  process_name VARCHAR(100) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  definition_version INT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  description VARCHAR(255) NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_wf_def_code_ver UNIQUE (process_code, definition_version)
);

-- ============================================================
-- Workflow process node definition
-- ============================================================
CREATE TABLE sys_workflow_node (
  id BIGINT NOT NULL AUTO_INCREMENT,
  workflow_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(100) NOT NULL,
  node_type VARCHAR(20) NOT NULL DEFAULT 'APPROVAL',
  sort_order INT NOT NULL DEFAULT 0,
  assignee_type VARCHAR(20) NOT NULL,
  assignee_value VARCHAR(255) NOT NULL,
  duration_hours INT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_wf_node_wf_code UNIQUE (workflow_id, node_code),
  CONSTRAINT fk_sys_wf_node_wf FOREIGN KEY (workflow_id) REFERENCES sys_workflow_definition (id)
);
CREATE INDEX idx_sys_wf_node_wf_id ON sys_workflow_node (workflow_id);

-- ============================================================
-- Approval instance (submitted approval records)
-- ============================================================
CREATE TABLE sys_approval_instance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  instance_code VARCHAR(100) NOT NULL,
  workflow_id BIGINT NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_id VARCHAR(100) NOT NULL,
  business_summary VARCHAR(255) NULL,
  submitted_by BIGINT NOT NULL,
  submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  snapshot_definition TEXT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_appr_inst_code UNIQUE (instance_code),
  CONSTRAINT fk_sys_appr_inst_wf FOREIGN KEY (workflow_id) REFERENCES sys_workflow_definition (id),
  CONSTRAINT fk_sys_appr_inst_submitter FOREIGN KEY (submitted_by) REFERENCES sys_user (id)
);
CREATE INDEX idx_sys_appr_inst_status ON sys_approval_instance (status);
CREATE INDEX idx_sys_appr_inst_biz_type ON sys_approval_instance (business_type);
CREATE INDEX idx_sys_appr_inst_submitter ON sys_approval_instance (submitted_by);
CREATE INDEX idx_sys_appr_inst_wf_id ON sys_approval_instance (workflow_id);

-- ============================================================
-- Approval node instance (per-node approval state)
-- ============================================================
CREATE TABLE sys_approval_node_instance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  instance_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(100) NOT NULL,
  assignee_type VARCHAR(20) NOT NULL,
  assignee_value VARCHAR(255) NOT NULL,
  actual_assignee BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  decision VARCHAR(500) NULL,
  decided_at TIMESTAMP(6) NULL,
  duration_hours INT NULL,
  deadline_at TIMESTAMP(6) NULL,
  overdue_notified BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(64) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_appr_ni_inst_code UNIQUE (instance_id, node_code),
  CONSTRAINT fk_sys_appr_ni_inst FOREIGN KEY (instance_id) REFERENCES sys_approval_instance (id),
  CONSTRAINT fk_sys_appr_ni_assignee FOREIGN KEY (actual_assignee) REFERENCES sys_user (id)
);
CREATE INDEX idx_sys_appr_ni_inst_id ON sys_approval_node_instance (instance_id);
CREATE INDEX idx_sys_appr_ni_assignee ON sys_approval_node_instance (actual_assignee);
CREATE INDEX idx_sys_appr_ni_status ON sys_approval_node_instance (status);

-- ============================================================
-- Unified task (approval, import, sync, etc.)
-- ============================================================
CREATE TABLE sys_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_type VARCHAR(64) NOT NULL,
  source_id VARCHAR(100) NOT NULL,
  title VARCHAR(255) NOT NULL,
  assignee_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
  business_url VARCHAR(500) NULL,
  sla_status VARCHAR(20) NOT NULL DEFAULT 'ON_TRACK',
  result_summary VARCHAR(500) NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  due_at TIMESTAMP(6) NULL,
  completed_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_task_idem UNIQUE (idempotency_key),
  CONSTRAINT fk_sys_task_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user (id)
);
CREATE INDEX idx_sys_task_assignee ON sys_task (assignee_id);
CREATE INDEX idx_sys_task_status ON sys_task (status);
CREATE INDEX idx_sys_task_src_type ON sys_task (source_type);
CREATE INDEX idx_sys_task_due_at ON sys_task (due_at);
CREATE INDEX idx_sys_task_idem ON sys_task (idempotency_key);

-- ============================================================
-- Task action log
-- ============================================================
CREATE TABLE sys_task_action_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  action_by BIGINT NOT NULL,
  action_detail VARCHAR(500) NULL,
  action_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_sys_task_log_task FOREIGN KEY (task_id) REFERENCES sys_task (id),
  CONSTRAINT fk_sys_task_log_user FOREIGN KEY (action_by) REFERENCES sys_user (id)
);
CREATE INDEX idx_sys_task_log_task_id ON sys_task_action_log (task_id);

-- ============================================================
-- In-site message
-- ============================================================
CREATE TABLE sys_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  message_type VARCHAR(32) NOT NULL DEFAULT 'INFO',
  status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
  source_type VARCHAR(64) NULL,
  source_id VARCHAR(100) NULL,
  business_url VARCHAR(500) NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  read_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_msg_idem UNIQUE (idempotency_key),
  CONSTRAINT fk_sys_msg_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user (id)
);
CREATE INDEX idx_sys_msg_recipient ON sys_message (recipient_id);
CREATE INDEX idx_sys_msg_status ON sys_message (status);
