-- SRM Stage 1 Productization: Organization & Permission management closure
-- Adds department/position/role/function-permission/data-permission menus,
-- permissions, dictionary seeds, schema extensions, and menu reordering.

-- ============================================================
-- Part 1: Schema extensions
-- ============================================================

ALTER TABLE sys_role ADD COLUMN role_category VARCHAR(32) NOT NULL DEFAULT 'BUSINESS';

ALTER TABLE sys_department ADD COLUMN manager_id BIGINT NULL;
ALTER TABLE sys_department ADD CONSTRAINT fk_sys_dept_manager FOREIGN KEY (manager_id) REFERENCES sys_user (id);

ALTER TABLE sys_position ADD COLUMN category VARCHAR(64) NULL;
CREATE INDEX idx_sys_position_category ON sys_position (category);

ALTER TABLE sys_role_data_policy ADD COLUMN scope_org_ids TEXT NULL COMMENT 'JSON array of organization IDs for ORG scope';

-- Update bootstrap roles with correct categories
UPDATE sys_role SET role_category = 'SYSTEM' WHERE role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN');
UPDATE sys_role SET role_category = 'BUSINESS' WHERE role_code IN ('MASTER_DATA_ADMIN', 'PROCESS_ADMIN');
UPDATE sys_role SET role_category = 'AUDIT' WHERE role_code IN ('INTERNAL_AUDITOR');
UPDATE sys_role SET role_category = 'BUSINESS' WHERE role_code IN ('INTERNAL_USER', 'SKELETON_VIEWER');

-- ============================================================
-- Part 2: Menu reordering — system management section
-- ============================================================

-- Hide old MENU_SYSTEM_PERMISSION (split into function-permission and data-permission)
UPDATE sys_menu SET visible = FALSE, enabled = FALSE WHERE menu_code = 'MENU_SYSTEM_PERMISSION';

-- Update existing system menus with new sort orders and labels
UPDATE sys_menu SET label = '内部用户', sort_order = 30, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_USER';
UPDATE sys_menu SET label = '角色管理', sort_order = 40, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_ROLE';
UPDATE sys_menu SET sort_order = 70, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_WORKFLOW';
UPDATE sys_menu SET sort_order = 80, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_DICTIONARY_PARAMETER';
UPDATE sys_menu SET sort_order = 90, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_TEMPLATE_ATTACHMENT';
UPDATE sys_menu SET sort_order = 100, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_MESSAGE';
UPDATE sys_menu SET sort_order = 110, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_AUDIT_LOG';
UPDATE sys_menu SET sort_order = 120, updated_by = 'flyway' WHERE menu_code = 'MENU_SYSTEM_INTEGRATION_JOB';

-- ============================================================
-- Part 3: New menu entries
-- ============================================================

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_DEPARTMENT', '部门管理', 'PAGE',
  '/system/department', 'SystemDepartment', 'system:department:view',
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_DEPARTMENT');

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_POSITION', '岗位管理', 'PAGE',
  '/system/position', 'SystemPosition', 'system:position:view',
  20, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_POSITION');

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_FUNCTION_PERMISSION', '功能权限', 'PAGE',
  '/system/function-permission', 'SystemFunctionPermission', 'system:function-permission:view',
  50, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_FUNCTION_PERMISSION');

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_DATA_PERMISSION', '数据权限', 'PAGE',
  '/system/data-permission', 'SystemDataPermission', 'system:data-permission:view',
  60, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_DATA_PERMISSION');

-- ============================================================
-- Part 4: New permissions
-- ============================================================

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
) VALUES
  ('system:department:view',          'system', 'department', 'view',          '查看部门',             1, TRUE, 'flyway', 'flyway'),
  ('system:department:create',        'system', 'department', 'create',        '新增部门',             1, TRUE, 'flyway', 'flyway'),
  ('system:department:update',        'system', 'department', 'update',        '编辑部门',             1, TRUE, 'flyway', 'flyway'),
  ('system:department:enable',        'system', 'department', 'enable',        '启用部门',             1, TRUE, 'flyway', 'flyway'),
  ('system:department:disable',       'system', 'department', 'disable',       '停用部门',             1, TRUE, 'flyway', 'flyway'),
  ('system:position:view',            'system', 'position', 'view',            '查看岗位',             1, TRUE, 'flyway', 'flyway'),
  ('system:position:create',          'system', 'position', 'create',          '新增岗位',             1, TRUE, 'flyway', 'flyway'),
  ('system:position:update',          'system', 'position', 'update',          '编辑岗位',             1, TRUE, 'flyway', 'flyway'),
  ('system:position:enable',          'system', 'position', 'enable',          '启用岗位',             1, TRUE, 'flyway', 'flyway'),
  ('system:position:disable',         'system', 'position', 'disable',         '停用岗位',             1, TRUE, 'flyway', 'flyway'),
  ('system:function-permission:view', 'system', 'function-permission', 'view', '查看功能权限',        1, TRUE, 'flyway', 'flyway'),
  ('system:data-permission:view',     'system', 'data-permission', 'view',     '查看数据权限',          1, TRUE, 'flyway', 'flyway'),
  ('system:data-permission:update',   'system', 'data-permission', 'update',   '编辑数据权限策略',       1, TRUE, 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ============================================================
-- Part 5: Grant new permissions to SUPER_ADMIN and SYSTEM_ADMIN
-- ============================================================

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:department:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:position:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:function-permission:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:data-permission:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:department:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:position:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:function-permission:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:data-permission:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ============================================================
-- Part 6: Grant new menus to SUPER_ADMIN and SYSTEM_ADMIN
-- ============================================================

INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN')
  AND m.visible = TRUE AND m.enabled = TRUE
  AND m.menu_code IN (
    'MENU_SYSTEM_DEPARTMENT', 'MENU_SYSTEM_POSITION',
    'MENU_SYSTEM_FUNCTION_PERMISSION', 'MENU_SYSTEM_DATA_PERMISSION'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

-- ============================================================
-- Part 7: Dictionary seeds for position category
-- ============================================================

INSERT INTO sys_dictionary (dict_code, dict_name, status, created_by, updated_by) VALUES
  ('POSITION_CATEGORY', '岗位类别', 'ACTIVE', 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO sys_dictionary_item (dict_id, item_code, item_name, sort_order, status, created_by, updated_by)
SELECT d.id, t.item_code, t.item_name, t.sort_order, 'ACTIVE', 'flyway', 'flyway'
FROM sys_dictionary d
CROSS JOIN (
  SELECT 'MANAGEMENT'  AS item_code, '管理岗'   AS item_name, 10 AS sort_order UNION ALL
  SELECT 'TECHNICAL',   '技术岗',       20                          UNION ALL
  SELECT 'PROCUREMENT', '采购岗',       30                          UNION ALL
  SELECT 'QUALITY',     '质量岗',       40                          UNION ALL
  SELECT 'LOGISTICS',   '物流岗',       50                          UNION ALL
  SELECT 'FINANCE',     '财务岗',       60                          UNION ALL
  SELECT 'ADMIN',       '行政岗',       70                          UNION ALL
  SELECT 'OTHER',       '其他',        80
) t
WHERE d.dict_code = 'POSITION_CATEGORY'
  AND NOT EXISTS (
    SELECT 1 FROM sys_dictionary_item di WHERE di.dict_id = d.id AND di.item_code = t.item_code
  );
