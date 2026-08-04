-- SRM Stage 1: Hide future-stage menus, insert stage-1 permissions, roles,
-- role-menu/permission assignments, dictionary seeds, and SUPER_ADMIN extension.
-- ============================================================
-- Part 1: Hide future-stage menus (phase > 1 directories and their children)
-- ============================================================
UPDATE sys_menu
SET visible = FALSE, enabled = FALSE
WHERE phase > 1 AND menu_type = 'DIRECTORY';

UPDATE sys_menu
SET visible = FALSE, enabled = FALSE
WHERE parent_id IN (
  SELECT t.id FROM (SELECT id FROM sys_menu WHERE phase > 1 AND menu_type = 'DIRECTORY') t
);

-- ============================================================
-- Part 2: Stage 1 menu items — update routes and component_keys to V9 spec;
--          use INSERT ... WHERE NOT EXISTS for deployment safety.
-- ============================================================
-- Masterdata pages
UPDATE sys_menu
SET route             = '/masterdata/organization',
    component_key     = 'MasterdataOrganization',
    sort_order        = 10,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_ORGANIZATION';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_ORGANIZATION', '企业组织', 'PAGE',
  '/masterdata/organization', 'MasterdataOrganization', 'masterdata:organization:view',
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_ORGANIZATION');

UPDATE sys_menu
SET route             = '/masterdata/purchasing-organization',
    component_key     = 'MasterdataPurchasingOrganization',
    sort_order        = 20,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_PURCHASING_ORG';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_PURCHASING_ORG', '采购组织', 'PAGE',
  '/masterdata/purchasing-organization', 'MasterdataPurchasingOrganization', 'masterdata:purchasing-organization:view',
  20, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_PURCHASING_ORG');

UPDATE sys_menu
SET route             = '/masterdata/plant-receiving',
    component_key     = 'MasterdataPlantReceiving',
    sort_order        = 30,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_PLANT_RECEIVING';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_PLANT_RECEIVING', '工厂/收货地点', 'PAGE',
  '/masterdata/plant-receiving', 'MasterdataPlantReceiving', 'masterdata:plant-receiving:view',
  30, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_PLANT_RECEIVING');

UPDATE sys_menu
SET route             = '/masterdata/material',
    component_key     = 'MasterdataMaterial',
    sort_order        = 40,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_MATERIAL';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_MATERIAL', '物料', 'PAGE',
  '/masterdata/material', 'MasterdataMaterial', 'masterdata:material:view',
  40, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_MATERIAL');

UPDATE sys_menu
SET route             = '/masterdata/category',
    component_key     = 'MasterdataCategory',
    sort_order        = 50,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_CATEGORY';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_CATEGORY', '品类', 'PAGE',
  '/masterdata/category', 'MasterdataCategory', 'masterdata:category:view',
  50, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_CATEGORY');

UPDATE sys_menu
SET route             = '/masterdata/unit-currency-tax',
    component_key     = 'MasterdataUnitCurrencyTax',
    sort_order        = 60,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_UNIT_CURRENCY_TAX';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_UNIT_CURRENCY_TAX', '单位/币种/税码', 'PAGE',
  '/masterdata/unit-currency-tax', 'MasterdataUnitCurrencyTax', 'masterdata:unit-currency-tax:view',
  60, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_UNIT_CURRENCY_TAX');

UPDATE sys_menu
SET route             = '/masterdata/delivery-location',
    component_key     = 'MasterdataDeliveryLocation',
    sort_order        = 70,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_DELIVERY_LOCATION';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_DELIVERY_LOCATION', '交付地点', 'PAGE',
  '/masterdata/delivery-location', 'MasterdataDeliveryLocation', 'masterdata:delivery-location:view',
  70, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_DELIVERY_LOCATION');

UPDATE sys_menu
SET route             = '/masterdata/external-mapping',
    component_key     = 'MasterdataExternalMapping',
    sort_order        = 80,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_MASTERDATA_EXTERNAL_MAPPING';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_EXTERNAL_MAPPING', '外围映射', 'PAGE',
  '/masterdata/external-mapping', 'MasterdataExternalMapping', 'masterdata:external-mapping:view',
  80, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA_EXTERNAL_MAPPING');

-- System pages
UPDATE sys_menu
SET route             = '/system/user',
    component_key     = 'SystemUser',
    sort_order        = 10,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_USER';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_USER', '用户', 'PAGE',
  '/system/user', 'SystemUser', 'system:user:view',
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_USER');

UPDATE sys_menu
SET route             = '/system/role',
    component_key     = 'SystemRole',
    sort_order        = 20,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_ROLE';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_ROLE', '角色与挂载用户', 'PAGE',
  '/system/role', 'SystemRole', 'system:role:view',
  20, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_ROLE');

UPDATE sys_menu
SET route             = '/system/permission',
    component_key     = 'SystemPermission',
    sort_order        = 30,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_PERMISSION';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_PERMISSION', '权限与数据域', 'PAGE',
  '/system/permission', 'SystemPermission', 'system:permission:view',
  30, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_PERMISSION');

UPDATE sys_menu
SET route             = '/system/workflow',
    component_key     = 'SystemWorkflow',
    sort_order        = 40,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_WORKFLOW';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_WORKFLOW', '流程', 'PAGE',
  '/system/workflow', 'SystemWorkflow', 'system:workflow:view',
  40, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_WORKFLOW');

UPDATE sys_menu
SET route             = '/system/dictionary-parameter',
    component_key     = 'SystemDictionaryParameter',
    sort_order        = 50,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_DICTIONARY_PARAMETER';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_DICTIONARY_PARAMETER', '字典参数', 'PAGE',
  '/system/dictionary-parameter', 'SystemDictionaryParameter', 'system:dictionary-parameter:view',
  50, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_DICTIONARY_PARAMETER');

UPDATE sys_menu
SET route             = '/system/template-attachment',
    component_key     = 'SystemTemplateAttachment',
    sort_order        = 60,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_TEMPLATE_ATTACHMENT';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_TEMPLATE_ATTACHMENT', '模板附件', 'PAGE',
  '/system/template-attachment', 'SystemTemplateAttachment', 'system:template-attachment:view',
  60, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_TEMPLATE_ATTACHMENT');

UPDATE sys_menu
SET route             = '/system/message',
    component_key     = 'SystemMessage',
    sort_order        = 70,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_MESSAGE';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_MESSAGE', '消息', 'PAGE',
  '/system/message', 'SystemMessage', 'system:message:view',
  70, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_MESSAGE');

UPDATE sys_menu
SET route             = '/system/audit-log',
    component_key     = 'SystemAuditLog',
    sort_order        = 80,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_AUDIT_LOG';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_AUDIT_LOG', '审计日志', 'PAGE',
  '/system/audit-log', 'SystemAuditLog', 'system:audit-log:view',
  80, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_AUDIT_LOG');

UPDATE sys_menu
SET route             = '/system/integration-job',
    component_key     = 'SystemIntegrationJob',
    sort_order        = 90,
    updated_by        = 'flyway'
WHERE menu_code = 'MENU_SYSTEM_INTEGRATION_JOB';

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_INTEGRATION_JOB', '接口与任务监控', 'PAGE',
  '/system/integration-job', 'SystemIntegrationJob', 'system:integration-job:view',
  90, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM'
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_INTEGRATION_JOB');

-- ============================================================
-- Part 3: Stage 1 permissions (CRUD and operation actions)
-- ============================================================

-- 3a: System permissions (explicit irregular patterns)
INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
) VALUES
  ('system:user:create',          'system', 'user',    'create',          '创建用户',             1, TRUE, 'flyway', 'flyway'),
  ('system:user:update',          'system', 'user',    'update',          '编辑用户',             1, TRUE, 'flyway', 'flyway'),
  ('system:user:enable',          'system', 'user',    'enable',          '启用用户',             1, TRUE, 'flyway', 'flyway'),
  ('system:user:disable',         'system', 'user',    'disable',         '停用用户',             1, TRUE, 'flyway', 'flyway'),
  ('system:user:reset-password',  'system', 'user',    'reset-password',  '重置用户密码',          1, TRUE, 'flyway', 'flyway'),
  ('system:role:create',          'system', 'role',    'create',          '创建角色',             1, TRUE, 'flyway', 'flyway'),
  ('system:role:update',          'system', 'role',    'update',          '编辑角色',             1, TRUE, 'flyway', 'flyway'),
  ('system:role:enable',          'system', 'role',    'enable',          '启用角色',             1, TRUE, 'flyway', 'flyway'),
  ('system:role:disable',         'system', 'role',    'disable',         '停用角色',             1, TRUE, 'flyway', 'flyway'),
  ('system:role:assign-user',     'system', 'role',    'assign-user',     '角色挂载用户',          1, TRUE, 'flyway', 'flyway'),
  ('system:role:assign-permission','system','role',    'assign-permission','角色分配权限',         1, TRUE, 'flyway', 'flyway'),
  ('system:role:assign-data-scope','system','role',    'assign-data-scope','角色分配数据域',        1, TRUE, 'flyway', 'flyway'),
  ('system:workflow:create',      'system', 'workflow','create',          '创建流程定义',          1, TRUE, 'flyway', 'flyway'),
  ('system:workflow:update',      'system', 'workflow','update',          '编辑流程定义',          1, TRUE, 'flyway', 'flyway'),
  ('system:workflow:publish',     'system', 'workflow','publish',         '发布流程',             1, TRUE, 'flyway', 'flyway'),
  ('system:workflow:retire',      'system', 'workflow','retire',          '退役流程',             1, TRUE, 'flyway', 'flyway'),
  ('system:approval:view',        'system', 'approval','view',            '查看审批',             1, TRUE, 'flyway', 'flyway'),
  ('system:approval:approve',     'system', 'approval','approve',         '审批通过',             1, TRUE, 'flyway', 'flyway'),
  ('system:approval:reject',      'system', 'approval','reject',          '审批驳回',             1, TRUE, 'flyway', 'flyway'),
  ('system:approval:withdraw',    'system', 'approval','withdraw',        '撤回审批',             1, TRUE, 'flyway', 'flyway'),
  ('system:task:view',            'system', 'task',    'view',            '查看任务',             1, TRUE, 'flyway', 'flyway'),
  ('system:task:manage',          'system', 'task',    'manage',          '管理任务',             1, TRUE, 'flyway', 'flyway'),
  ('system:message:manage',       'system', 'message', 'manage',          '管理消息',             1, TRUE, 'flyway', 'flyway'),
  ('system:dictionary:create',    'system', 'dictionary','create',        '创建字典',             1, TRUE, 'flyway', 'flyway'),
  ('system:dictionary:update',    'system', 'dictionary','update',        '编辑字典',             1, TRUE, 'flyway', 'flyway'),
  ('system:dictionary:enable',    'system', 'dictionary','enable',        '启用字典',             1, TRUE, 'flyway', 'flyway'),
  ('system:dictionary:disable',   'system', 'dictionary','disable',       '停用字典',             1, TRUE, 'flyway', 'flyway'),
  ('system:parameter:create',     'system', 'parameter','create',         '创建参数',             1, TRUE, 'flyway', 'flyway'),
  ('system:parameter:update',     'system', 'parameter','update',         '编辑参数',             1, TRUE, 'flyway', 'flyway'),
  ('system:parameter:submit',     'system', 'parameter','submit',         '提交参数版本',          1, TRUE, 'flyway', 'flyway'),
  ('system:number-rule:create',   'system', 'number-rule','create',       '创建编号规则',          1, TRUE, 'flyway', 'flyway'),
  ('system:number-rule:update',   'system', 'number-rule','update',       '编辑编号规则',          1, TRUE, 'flyway', 'flyway'),
  ('system:number-rule:enable',   'system', 'number-rule','enable',       '启用编号规则',          1, TRUE, 'flyway', 'flyway'),
  ('system:number-rule:disable',  'system', 'number-rule','disable',      '停用编号规则',          1, TRUE, 'flyway', 'flyway'),
  ('system:attachment:view',      'system', 'attachment','view',          '查看附件',             1, TRUE, 'flyway', 'flyway'),
  ('system:attachment:upload',    'system', 'attachment','upload',        '上传附件',             1, TRUE, 'flyway', 'flyway'),
  ('system:attachment:download',  'system', 'attachment','download',      '下载附件',             1, TRUE, 'flyway', 'flyway'),
  ('system:attachment:delete',    'system', 'attachment','delete',        '删除附件',             1, TRUE, 'flyway', 'flyway'),
  ('system:audit-log:export',     'system', 'audit-log','export',         '导出审计日志',          1, TRUE, 'flyway', 'flyway'),
  ('system:integration-job:retry','system', 'integration-job','retry',    '重试集成任务',          1, TRUE, 'flyway', 'flyway'),
  ('system:batch-job:view',       'system', 'batch-job','view',           '查看批处理任务',        1, TRUE, 'flyway', 'flyway'),
  ('system:workflow:view',        'system', 'workflow','view',            '查看流程',             1, TRUE, 'flyway', 'flyway'),
  ('system:dictionary:view',      'system', 'dictionary','view',          '查看字典',             1, TRUE, 'flyway', 'flyway'),
  ('system:parameter:view',       'system', 'parameter','view',           '查看参数',             1, TRUE, 'flyway', 'flyway'),
  ('system:number-rule:view',     'system', 'number-rule','view',         '查看编号规则',          1, TRUE, 'flyway', 'flyway'),
  ('system:template-attachment:view','system','template-attachment','view','查看模板附件',        1, TRUE, 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Also ensure system:message:view and system:permission:view exist (from V3, fallback)
INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
) VALUES
  ('system:message:view',   'system', 'message',           'view', '查看消息',           1, TRUE, 'flyway', 'flyway'),
  ('system:permission:view', 'system', 'permission',       'view', '查看权限与数据域',    1, TRUE, 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 3b: Masterdata permissions (resource x action matrix)
INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
)
SELECT x.permission_code, x.domain_code, x.resource_code, x.action_code, x.description,
       x.phase, x.enabled, x.created_by, x.updated_by
FROM (
  SELECT
    CONCAT('masterdata:', r.resource_code, ':', a.action_code) AS permission_code,
    'masterdata' AS domain_code,
    r.resource_code,
    a.action_code,
    CONCAT(a.label, r.label) AS description,
    1 AS phase, TRUE AS enabled, 'flyway' AS created_by, 'flyway' AS updated_by
  FROM (
    SELECT 'organization'           AS resource_code, '企业组织'   AS label UNION ALL
    SELECT 'purchasing-organization','采购组织'                     UNION ALL
    SELECT 'plant',                  '工厂'                        UNION ALL
    SELECT 'warehouse',              '仓库'                        UNION ALL
    SELECT 'delivery-location',      '交付地点'                     UNION ALL
    SELECT 'material',               '物料'                        UNION ALL
    SELECT 'category',               '品类'                        UNION ALL
    SELECT 'unit',                   '单位'                        UNION ALL
    SELECT 'currency',               '币种'                        UNION ALL
    SELECT 'tax-code',               '税码'                        UNION ALL
    SELECT 'external-mapping',       '外围映射'
  ) r
  CROSS JOIN (
    SELECT 'view'    AS action_code, '查看' AS label UNION ALL
    SELECT 'create',  '创建'                         UNION ALL
    SELECT 'update',  '编辑'                         UNION ALL
    SELECT 'enable',  '启用'                         UNION ALL
    SELECT 'disable', '停用'                         UNION ALL
    SELECT 'import',  '导入'                         UNION ALL
    SELECT 'export',  '导出'                         UNION ALL
    SELECT 'sync',    '同步'
  ) a
) x
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  phase, enabled, created_by, updated_by
) VALUES (
  'masterdata:organization:sort', 'masterdata', 'organization', 'sort',
  '排序企业组织', 1, TRUE, 'flyway', 'flyway'
) ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ============================================================
-- Part 4: Stage 1 role seeds
-- ============================================================
INSERT INTO sys_role (
  role_code, role_name, description, status, built_in, created_by, updated_by
) VALUES
  ('SYSTEM_ADMIN',      '系统管理员',  '用户、角色、流程、字典参数、消息和系统配置管理', 'ACTIVE', TRUE, 'flyway', 'flyway'),
  ('MASTER_DATA_ADMIN', '主数据管理员','阶段1主数据维护、导入和同步处理',               'ACTIVE', TRUE, 'flyway', 'flyway'),
  ('PROCESS_ADMIN',     '流程管理员',  '流程定义、任务和SLA管理',                      'ACTIVE', TRUE, 'flyway', 'flyway'),
  ('INTERNAL_AUDITOR',  '内部审计员',  '授权范围内只读查询、审计和导出',                 'ACTIVE', TRUE, 'flyway', 'flyway'),
  ('INTERNAL_USER',     '内部用户',    '本人工作台、任务和消息',                       'ACTIVE', TRUE, 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ============================================================
-- Part 5: Role-menu assignments (visible and enabled menus only)
-- ============================================================
-- SYSTEM_ADMIN: workbench + children, system + children, masterdata + children
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SYSTEM_ADMIN' AND m.visible = TRUE AND m.enabled = TRUE
  AND (
    m.menu_code LIKE 'MENU_WORKBENCH%'
    OR m.menu_code LIKE 'MENU_SYSTEM%'
    OR m.menu_code LIKE 'MENU_MASTERDATA%'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

-- MASTER_DATA_ADMIN: workbench + children, masterdata + children
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'MASTER_DATA_ADMIN' AND m.visible = TRUE AND m.enabled = TRUE
  AND (
    m.menu_code LIKE 'MENU_WORKBENCH%'
    OR m.menu_code LIKE 'MENU_MASTERDATA%'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

-- PROCESS_ADMIN: workbench + children, system directory + workflow/message/audit-log
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'PROCESS_ADMIN' AND m.visible = TRUE AND m.enabled = TRUE
  AND (
    m.menu_code LIKE 'MENU_WORKBENCH%'
    OR m.menu_code = 'MENU_SYSTEM'
    OR m.menu_code IN ('MENU_SYSTEM_WORKFLOW', 'MENU_SYSTEM_MESSAGE', 'MENU_SYSTEM_AUDIT_LOG')
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

-- INTERNAL_AUDITOR: workbench + children, system directory + audit-log, masterdata + children
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'INTERNAL_AUDITOR' AND m.visible = TRUE AND m.enabled = TRUE
  AND (
    m.menu_code LIKE 'MENU_WORKBENCH%'
    OR m.menu_code = 'MENU_SYSTEM'
    OR m.menu_code = 'MENU_SYSTEM_AUDIT_LOG'
    OR m.menu_code LIKE 'MENU_MASTERDATA%'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

-- INTERNAL_USER: workbench + children, system directory + message
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'INTERNAL_USER' AND m.visible = TRUE AND m.enabled = TRUE
  AND (
    m.menu_code LIKE 'MENU_WORKBENCH%'
    OR m.menu_code = 'MENU_SYSTEM'
    OR m.menu_code = 'MENU_SYSTEM_MESSAGE'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

-- ============================================================
-- Part 6: Role-permission assignments
-- ============================================================
-- SYSTEM_ADMIN: all system:* permissions
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'system:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- MASTER_DATA_ADMIN: all masterdata:* permissions
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'MASTER_DATA_ADMIN' AND p.enabled = TRUE
  AND p.permission_code LIKE 'masterdata:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- PROCESS_ADMIN: system:workflow:*, system:task:*, system:approval:view
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'PROCESS_ADMIN' AND p.enabled = TRUE
  AND (
    p.permission_code LIKE 'system:workflow:%'
    OR p.permission_code LIKE 'system:task:%'
    OR p.permission_code = 'system:approval:view'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- INTERNAL_AUDITOR: all *:view and export permissions
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'INTERNAL_AUDITOR' AND p.enabled = TRUE
  AND (
    p.permission_code LIKE '%:view'
    OR p.permission_code LIKE '%:export'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- INTERNAL_USER: workbench:home:view, system:message:view, system:task:view, system:approval:view
INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'INTERNAL_USER' AND p.enabled = TRUE
  AND p.permission_code IN (
    'workbench:home:view', 'system:message:view', 'system:task:view', 'system:approval:view'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ============================================================
-- Part 7: Dictionary seeds
-- ============================================================
INSERT INTO sys_dictionary (dict_code, dict_name, status, created_by, updated_by) VALUES
  ('ORG_TYPE',    '组织类型',   'ACTIVE', 'flyway', 'flyway'),
  ('USER_STATUS', '用户状态',  'ACTIVE', 'flyway', 'flyway'),
  ('MATERIAL_TYPE','物料类型',  'ACTIVE', 'flyway', 'flyway')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

-- ORG_TYPE items
INSERT INTO sys_dictionary_item (dict_id, item_code, item_name, sort_order, status, created_by, updated_by)
SELECT d.id, t.item_code, t.item_name, t.sort_order, 'ACTIVE', 'flyway', 'flyway'
FROM sys_dictionary d
CROSS JOIN (
  SELECT 'GROUP'           AS item_code, '集团'     AS item_name, 10 AS sort_order UNION ALL
  SELECT 'COMPANY',         '公司',       20                          UNION ALL
  SELECT 'BUSINESS_UNIT',   '业务单元',    30                          UNION ALL
  SELECT 'PLANT',           '工厂',       40                          UNION ALL
  SELECT 'WAREHOUSE',       '仓库',       50
) t
WHERE d.dict_code = 'ORG_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_dictionary_item di WHERE di.dict_id = d.id AND di.item_code = t.item_code
  );

-- USER_STATUS items
INSERT INTO sys_dictionary_item (dict_id, item_code, item_name, sort_order, status, created_by, updated_by)
SELECT d.id, t.item_code, t.item_name, t.sort_order, 'ACTIVE', 'flyway', 'flyway'
FROM sys_dictionary d
CROSS JOIN (
  SELECT 'ACTIVE'   AS item_code, '启用' AS item_name, 10 AS sort_order UNION ALL
  SELECT 'INACTIVE', '停用',       20
) t
WHERE d.dict_code = 'USER_STATUS'
  AND NOT EXISTS (
    SELECT 1 FROM sys_dictionary_item di WHERE di.dict_id = d.id AND di.item_code = t.item_code
  );

-- MATERIAL_TYPE items
INSERT INTO sys_dictionary_item (dict_id, item_code, item_name, sort_order, status, created_by, updated_by)
SELECT d.id, t.item_code, t.item_name, t.sort_order, 'ACTIVE', 'flyway', 'flyway'
FROM sys_dictionary d
CROSS JOIN (
  SELECT 'RAW_MATERIAL'   AS item_code, '原材料'   AS item_name, 10 AS sort_order UNION ALL
  SELECT 'FINISHED_GOOD',  '产成品',      20                          UNION ALL
  SELECT 'SEMI_FINISHED',  '半成品',      30                          UNION ALL
  SELECT 'SERVICE',        '服务',       40                          UNION ALL
  SELECT 'PACKAGING',      '包装',       50
) t
WHERE d.dict_code = 'MATERIAL_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_dictionary_item di WHERE di.dict_id = d.id AND di.item_code = t.item_code
  );

-- ============================================================
-- Part 8: Update SUPER_ADMIN — ensure all stage-1 (visible) menus
--         and all enabled permissions are assigned.
-- ============================================================
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN' AND m.visible = TRUE AND m.enabled = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu srm WHERE srm.role_id = r.id AND srm.menu_id = m.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission srp WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ============================================================
-- Part 9: SUPER_ADMIN data policies — ALL + READ_WRITE on all stage-1 dimensions
--         (default deny otherwise; SUPER_ADMIN is the platform-wide administrator)
-- ============================================================
INSERT INTO sys_role_data_policy (role_id, domain_code, dimension_code, scope_type, include_children, operation_mode, status, created_by, created_at, updated_by, updated_at, version)
SELECT r.id, d.domain_code, d.dimension_code, 'ALL', TRUE, 'READ_WRITE', 'ACTIVE', 'flyway', NOW(6), 'flyway', NOW(6), 0
FROM sys_role r
CROSS JOIN (
  SELECT 'masterdata' AS domain_code, 'ORGANIZATION' AS dimension_code UNION ALL
  SELECT 'masterdata', 'PURCHASING_ORGANIZATION' UNION ALL
  SELECT 'masterdata', 'PLANT' UNION ALL
  SELECT 'masterdata', 'CATEGORY' UNION ALL
  SELECT 'masterdata', 'OWNER' UNION ALL
  SELECT 'system', 'ORGANIZATION' UNION ALL
  SELECT 'system', 'OWNER'
) d
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_data_policy p
    WHERE p.role_id = r.id AND p.domain_code = d.domain_code AND p.dimension_code = d.dimension_code
  );
