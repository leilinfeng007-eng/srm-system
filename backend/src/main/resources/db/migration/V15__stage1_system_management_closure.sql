-- SRM Stage 1 System Management Closure: close deterministic authorization gaps
-- for the remaining system management functions.
-- 1) PROCESS_ADMIN can approve/reject/withdraw approvals and view/manage own messages
--    (menus were granted in V9 but matching permissions were missing -> broken grants).
-- 2) MASTER_DATA_ADMIN gains page access to 模板附件 (templates/attachments/batch jobs),
--    matching the permissions already granted in V10/V11.
-- No schema change is required; this is a forward-only grant migration.

-- ============================================================
-- Part 1: PROCESS_ADMIN approval operations
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'PROCESS_ADMIN' AND p.enabled = TRUE
  AND p.permission_code IN (
    'system:approval:approve',
    'system:approval:reject',
    'system:approval:withdraw'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ============================================================
-- Part 2: PROCESS_ADMIN message view/manage (own messages only;
--         service layer still enforces recipient_id)
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'PROCESS_ADMIN' AND p.enabled = TRUE
  AND p.permission_code IN (
    'system:message:view',
    'system:message:manage'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ============================================================
-- Part 3: MASTER_DATA_ADMIN page access to 模板附件
-- (system directory + template-attachment page; template/attachment/
--  batch-job action permissions were already granted in V10/V11)
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'MASTER_DATA_ADMIN'
  AND m.visible = TRUE AND m.enabled = TRUE
  AND m.menu_code IN (
    'MENU_SYSTEM',
    'MENU_SYSTEM_TEMPLATE_ATTACHMENT'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'MASTER_DATA_ADMIN' AND p.enabled = TRUE
  AND p.permission_code = 'system:template-attachment:view'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ============================================================
-- Part 4: MASTER_DATA_ADMIN system data scope for shared
-- governance objects (document templates and their attachments
-- resolve to the system/ORGANIZATION dimension). Batch jobs and
-- own files remain covered by the existing system/OWNER SELF policy.
-- ============================================================
INSERT INTO sys_role_data_policy (
  role_id, domain_code, dimension_code, scope_type, include_children,
  operation_mode, status, created_by, updated_by
)
SELECT r.id, 'system', 'ORGANIZATION', 'ALL', TRUE,
       'READ_WRITE', 'ACTIVE', 'flyway', 'flyway'
FROM sys_role r
WHERE r.role_code = 'MASTER_DATA_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_data_policy existing
    WHERE existing.role_id = r.id
      AND existing.domain_code = 'system'
      AND existing.dimension_code = 'ORGANIZATION'
  );

-- ============================================================
-- Part 5: MASTER_DATA_ADMIN document template operations
-- (role description includes 模板; V10 only granted these to
--  SUPER_ADMIN/SYSTEM_ADMIN, leaving the template page 403)
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT role.id, permission.id, 'flyway'
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code LIKE 'system:document-template:%'
 AND permission.enabled = TRUE
WHERE role.role_code = 'MASTER_DATA_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );