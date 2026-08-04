-- SRM Stage 1 remediation: authorization paths, workflow versioning and integration retry state.

ALTER TABLE sys_inbox_event
  ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE sys_inbox_event
  ADD COLUMN max_attempts INT NOT NULL DEFAULT 5;
ALTER TABLE sys_inbox_event
  ADD COLUMN next_retry_at TIMESTAMP(6) NULL;
ALTER TABLE sys_inbox_event
  ADD COLUMN last_error VARCHAR(500) NULL;
ALTER TABLE sys_inbox_event
  ADD COLUMN processing_started_at TIMESTAMP(6) NULL;

ALTER TABLE sys_inbox_event
  MODIFY COLUMN payload_summary TEXT NULL;

CREATE INDEX idx_sys_inbox_retry ON sys_inbox_event (status, next_retry_at);

CREATE UNIQUE INDEX uk_md_dl_src_ext
  ON md_delivery_location (source_system, external_id);
CREATE UNIQUE INDEX uk_md_cat_src_ext
  ON md_category (source_system, external_id);
CREATE UNIQUE INDEX uk_md_unit_src_ext
  ON md_unit (source_system, external_id);
CREATE UNIQUE INDEX uk_md_curr_src_ext
  ON md_currency (source_system, external_id);
CREATE UNIQUE INDEX uk_md_tax_src_ext
  ON md_tax_code (source_system, external_id);
CREATE UNIQUE INDEX uk_md_mat_src_ext
  ON md_material (source_system, external_id);

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
)
SELECT 'system:approval:cancel', 'system', 'approval', 'cancel',
       '取消审批实例', 1, TRUE, 'flyway', 'flyway'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'system:approval:cancel'
);

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
)
SELECT permission_code, 'system', resource_code, action_code, description,
       1, TRUE, 'flyway', 'flyway'
FROM (
  SELECT 'system:document-template:create' permission_code, 'document-template' resource_code, 'create' action_code, '创建文档模板版本' description
  UNION ALL SELECT 'system:document-template:update', 'document-template', 'update', '维护模板草稿'
  UNION ALL SELECT 'system:document-template:publish', 'document-template', 'publish', '发布文档模板'
  UNION ALL SELECT 'system:document-template:disable', 'document-template', 'disable', '停用文档模板'
  UNION ALL SELECT 'system:document-template:download', 'document-template', 'download', '下载文档模板'
) permissions
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission existing
  WHERE existing.permission_code = permissions.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT role.id, permission.id, 'flyway'
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code LIKE 'system:document-template:%'
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT role.id, permission.id, 'flyway'
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'system:approval:cancel'
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'PROCESS_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

-- Formal stage-1 roles need explicit data policies. Absence still means deny.
INSERT INTO sys_role_data_policy (
  role_id, domain_code, dimension_code, scope_type, include_children,
  operation_mode, status, created_by, updated_by
)
SELECT r.id, p.domain_code, p.dimension_code, p.scope_type, TRUE,
       p.operation_mode, 'ACTIVE', 'flyway', 'flyway'
FROM sys_role r
JOIN (
  SELECT 'SYSTEM_ADMIN' AS role_code, 'system' AS domain_code,
         'ORGANIZATION' AS dimension_code, 'ALL' AS scope_type,
         'READ_WRITE' AS operation_mode
  UNION ALL SELECT 'SYSTEM_ADMIN', 'system', 'OWNER', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'MASTER_DATA_ADMIN', 'masterdata', 'ORGANIZATION', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'MASTER_DATA_ADMIN', 'masterdata', 'PURCHASING_ORGANIZATION', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'MASTER_DATA_ADMIN', 'masterdata', 'PLANT', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'MASTER_DATA_ADMIN', 'masterdata', 'CATEGORY', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'MASTER_DATA_ADMIN', 'masterdata', 'OWNER', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'PROCESS_ADMIN', 'system', 'ORGANIZATION', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'PROCESS_ADMIN', 'system', 'OWNER', 'ALL', 'READ_WRITE'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'system', 'ORGANIZATION', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'system', 'OWNER', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'masterdata', 'ORGANIZATION', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'masterdata', 'PURCHASING_ORGANIZATION', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'masterdata', 'PLANT', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'masterdata', 'CATEGORY', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_AUDITOR', 'masterdata', 'OWNER', 'ALL', 'READONLY'
  UNION ALL SELECT 'INTERNAL_USER', 'system', 'OWNER', 'SELF', 'READONLY'
) p ON p.role_code = r.role_code
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_data_policy existing
  WHERE existing.role_id = r.id
    AND existing.domain_code = p.domain_code
    AND existing.dimension_code = p.dimension_code
);

-- Master-data administrators manage attachments owned by master-data records.
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, permission.id, 'flyway'
FROM sys_role r
JOIN sys_permission permission ON permission.permission_code IN (
  'system:attachment:view', 'system:attachment:upload',
  'system:attachment:download', 'system:attachment:delete',
  'system:batch-job:view', 'system:integration-job:view',
  'system:integration-job:retry'
)
WHERE r.role_code = 'MASTER_DATA_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = r.id AND existing.permission_id = permission.id
  );
