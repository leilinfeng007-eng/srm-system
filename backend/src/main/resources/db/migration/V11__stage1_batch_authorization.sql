-- Stage 1 batch execution permissions and owner-scoped master-data administration.
INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code,
  description, phase, enabled, created_by, updated_by
)
SELECT p.permission_code, 'system', 'batch-job', p.action_code,
       p.description, 1, TRUE, 'flyway', 'flyway'
FROM (
  SELECT 'system:batch-job:import' permission_code, 'import' action_code, '执行主数据批量导入' description
  UNION ALL SELECT 'system:batch-job:export', 'export', '执行主数据范围内导出'
  UNION ALL SELECT 'system:batch-job:retry', 'retry', '重试失败的批量任务'
) p
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission existing WHERE existing.permission_code = p.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT role.id, permission.id, 'flyway'
FROM sys_role role
JOIN sys_permission permission ON permission.permission_code IN (
  'system:batch-job:import', 'system:batch-job:export', 'system:batch-job:retry'
)
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'MASTER_DATA_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

INSERT INTO sys_role_data_policy (
  role_id, domain_code, dimension_code, scope_type, include_children,
  operation_mode, status, created_by, updated_by
)
SELECT role.id, 'system', 'OWNER', 'SELF', FALSE,
       'READ_WRITE', 'ACTIVE', 'flyway', 'flyway'
FROM sys_role role
WHERE role.role_code = 'MASTER_DATA_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_data_policy existing
    WHERE existing.role_id = role.id AND existing.domain_code = 'system'
      AND existing.dimension_code = 'OWNER'
  );
