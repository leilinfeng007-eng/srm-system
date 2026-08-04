-- Users who can view their own messages must also be able to mark those same
-- owner-filtered messages as read. The service still enforces recipient_id.
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT role.id, permission.id, 'flyway'
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code = 'system:message:manage'
 AND permission.enabled = TRUE
WHERE role.role_code = 'INTERNAL_USER'
  AND role.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permission grant_row
    WHERE grant_row.role_id = role.id
      AND grant_row.permission_id = permission.id
  );
