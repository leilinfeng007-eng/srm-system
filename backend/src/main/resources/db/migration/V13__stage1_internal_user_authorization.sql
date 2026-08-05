-- SRM Stage 1 Internal User closure: menu naming, authorization permissions,
-- role grants for existing roles, and user role relationship re-activation safety.

-- ============================================================
-- Part 1: Rename system user menu to 内部用户
-- ============================================================
UPDATE sys_menu
SET label      = '内部用户',
    updated_by = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_USER';

-- ============================================================
-- Part 2: New incremental authorization permissions
-- ============================================================
INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
) VALUES
  ('system:user:assign-role',         'system', 'user', 'assign-role',         '配置用户角色',              1, TRUE, 'flyway', 'flyway'),
  ('system:user:view-permissions',    'system', 'user', 'view-permissions',    '查看用户权限',              1, TRUE, 'flyway', 'flyway'),
  ('system:user:view-authorization',  'system', 'user', 'view-authorization',  '查看授权记录',              1, TRUE, 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ============================================================
-- Part 3: Grant new permissions to existing roles
-- SYSTEM_ADMIN: all system:* permissions (incremental, matching V9 part 6)
-- SUPER_ADMIN: all enabled permissions (incremental, matching V9 part 8)
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = TRUE
  AND p.permission_code IN (
    'system:user:assign-role',
    'system:user:view-permissions',
    'system:user:view-authorization'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE
  AND p.permission_code IN (
    'system:user:assign-role',
    'system:user:view-permissions',
    'system:user:view-authorization'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
