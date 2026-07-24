-- Generated from frontend/module-manifest.json by scripts/generate-skeleton.mjs.
-- Do not hand-edit this migration; update the manifest before the migration is released.

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_WORKBENCH', '工作台', 'DIRECTORY', NULL, NULL, NULL,
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_SUPPLIER', '供应商管理', 'DIRECTORY', NULL, NULL, NULL,
  20, 2, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_SOURCING', '战略寻源', 'DIRECTORY', NULL, NULL, NULL,
  30, 4, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_CONTRACT', '合同与价格管理', 'DIRECTORY', NULL, NULL, NULL,
  40, 5, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_SOURCE', '供应源管理', 'DIRECTORY', NULL, NULL, NULL,
  50, 3, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_PROCUREMENT', '采购协同', 'DIRECTORY', NULL, NULL, NULL,
  60, 6, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_DELIVERY', '交付与收货协同', 'DIRECTORY', NULL, NULL, NULL,
  70, 7, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_QUALITY', '供应商质量管理', 'DIRECTORY', NULL, NULL, NULL,
  80, 8, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_SETTLEMENT', '对账与结算协同', 'DIRECTORY', NULL, NULL, NULL,
  90, 9, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_PERFORMANCE', '绩效与风险管理', 'DIRECTORY', NULL, NULL, NULL,
  100, 10, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_MASTERDATA', '基础数据中心', 'DIRECTORY', NULL, NULL, NULL,
  110, 1, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) VALUES (
  'MENU_SYSTEM', '系统管理', 'DIRECTORY', NULL, NULL, NULL,
  120, 1, TRUE, TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_WORKBENCH_HOME', '工作台首页', 'PAGE',
  '/workbench', 'workbench/home/index', 'workbench:home:view',
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_WORKBENCH';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'workbench:home:view', 'workbench',
  'home', 'view',
  '查看工作台首页', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_POOL', '供应商资源池', 'PAGE',
  '/supplier/pool', 'supplier/pool/index', 'supplier:pool:view',
  10, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:pool:view', 'supplier',
  'pool', 'view',
  '查看供应商资源池', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_REGISTRATION', '注册申请', 'PAGE',
  '/supplier/registrations', 'supplier/registration/index', 'supplier:registration:view',
  20, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:registration:view', 'supplier',
  'registration', 'view',
  '查看注册申请', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_PROFILE', '供应商档案', 'PAGE',
  '/supplier/profiles', 'supplier/profile/index', 'supplier:profile:view',
  30, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:profile:view', 'supplier',
  'profile', 'view',
  '查看供应商档案', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_ADMISSION', '准入与认证', 'PAGE',
  '/supplier/admissions', 'supplier/admission/index', 'supplier:admission:view',
  40, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:admission:view', 'supplier',
  'admission', 'view',
  '查看准入与认证', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_QUALIFICATION', '资质证照', 'PAGE',
  '/supplier/qualifications', 'supplier/qualification/index', 'supplier:qualification:view',
  50, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:qualification:view', 'supplier',
  'qualification', 'view',
  '查看资质证照', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_REVIEW_CHANGE', '复审与变更', 'PAGE',
  '/supplier/reviews-changes', 'supplier/review-change/index', 'supplier:review-change:view',
  60, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:review-change:view', 'supplier',
  'review-change', 'view',
  '查看复审与变更', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_RESTRICTION', '冻结与退出', 'PAGE',
  '/supplier/restrictions', 'supplier/restriction/index', 'supplier:restriction:view',
  70, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:restriction:view', 'supplier',
  'restriction', 'view',
  '查看冻结与退出', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SUPPLIER_USER', '供应商用户', 'PAGE',
  '/supplier/users', 'supplier/user/index', 'supplier:user:view',
  80, 2, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SUPPLIER';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'supplier:user:view', 'supplier',
  'user', 'view',
  '查看供应商用户', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_REQUIREMENT', '寻源需求', 'PAGE',
  '/sourcing/requirements', 'sourcing/requirement/index', 'sourcing:requirement:view',
  10, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:requirement:view', 'sourcing',
  'requirement', 'view',
  '查看寻源需求', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_PROJECT', '寻源项目', 'PAGE',
  '/sourcing/projects', 'sourcing/project/index', 'sourcing:project:view',
  20, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:project:view', 'sourcing',
  'project', 'view',
  '查看寻源项目', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_RFX', 'RFI/RFQ', 'PAGE',
  '/sourcing/rfx', 'sourcing/rfx/index', 'sourcing:rfx:view',
  30, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:rfx:view', 'sourcing',
  'rfx', 'view',
  '查看RFI/RFQ', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_BID_AUCTION', '招投标/竞价', 'PAGE',
  '/sourcing/bids-auctions', 'sourcing/bid-auction/index', 'sourcing:bid-auction:view',
  40, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:bid-auction:view', 'sourcing',
  'bid-auction', 'view',
  '查看招投标/竞价', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_QUOTATION', '供应商报价', 'PAGE',
  '/sourcing/quotations', 'sourcing/quotation/index', 'sourcing:quotation:view',
  50, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:quotation:view', 'sourcing',
  'quotation', 'view',
  '查看供应商报价', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_CLARIFICATION', '澄清答疑', 'PAGE',
  '/sourcing/clarifications', 'sourcing/clarification/index', 'sourcing:clarification:view',
  60, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:clarification:view', 'sourcing',
  'clarification', 'view',
  '查看澄清答疑', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_EVALUATION_AWARD', '评审与定标', 'PAGE',
  '/sourcing/evaluations-awards', 'sourcing/evaluation-award/index', 'sourcing:evaluation-award:view',
  70, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:evaluation-award:view', 'sourcing',
  'evaluation-award', 'view',
  '查看评审与定标', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCING_CONVERSION', '中选转化任务', 'PAGE',
  '/sourcing/conversions', 'sourcing/conversion/index', 'sourcing:conversion:view',
  80, 4, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCING';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'sourcing:conversion:view', 'sourcing',
  'conversion', 'view',
  '查看中选转化任务', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_CONTRACT', '合同台账', 'PAGE',
  '/contract/contracts', 'contract/contract/index', 'contract:contract:view',
  10, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:contract:view', 'contract',
  'contract', 'view',
  '查看合同台账', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_AGREEMENT', '框架/补充协议', 'PAGE',
  '/contract/agreements', 'contract/agreement/index', 'contract:agreement:view',
  20, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:agreement:view', 'contract',
  'agreement', 'view',
  '查看框架/补充协议', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_PURCHASING_BASIS', '采购依据', 'PAGE',
  '/contract/purchasing-bases', 'contract/purchasing-basis/index', 'contract:purchasing-basis:view',
  30, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:purchasing-basis:view', 'contract',
  'purchasing-basis', 'view',
  '查看采购依据', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_PRICE_CONDITION', '价格条件', 'PAGE',
  '/contract/price-conditions', 'contract/price-condition/index', 'contract:price-condition:view',
  40, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:price-condition:view', 'contract',
  'price-condition', 'view',
  '查看价格条件', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_PRICE_VERSION', '调价与版本', 'PAGE',
  '/contract/price-versions', 'contract/price-version/index', 'contract:price-version:view',
  50, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:price-version:view', 'contract',
  'price-version', 'view',
  '查看调价与版本', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_RENEWAL_TERMINATION', '续签/终止', 'PAGE',
  '/contract/renewals-terminations', 'contract/renewal-termination/index', 'contract:renewal-termination:view',
  60, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:renewal-termination:view', 'contract',
  'renewal-termination', 'view',
  '查看续签/终止', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_CONTRACT_PERFORMANCE_ALERT', '履约与临期预警', 'PAGE',
  '/contract/performance-alerts', 'contract/performance-alert/index', 'contract:performance-alert:view',
  70, 5, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_CONTRACT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'contract:performance-alert:view', 'contract',
  'performance-alert', 'view',
  '查看履约与临期预警', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_CANDIDATE', '候选供应源', 'PAGE',
  '/supply-source/candidates', 'source/candidate/index', 'source:candidate:view',
  10, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:candidate:view', 'source',
  'candidate', 'view',
  '查看候选供应源', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_TRIAL_CONDITIONAL', '试制/条件供应源', 'PAGE',
  '/supply-source/trial-conditional', 'source/trial-conditional/index', 'source:trial-conditional:view',
  20, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:trial-conditional:view', 'source',
  'trial-conditional', 'view',
  '查看试制/条件供应源', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_APPROVED', '正式供应源', 'PAGE',
  '/supply-source/approved', 'source/approved/index', 'source:approved:view',
  30, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:approved:view', 'source',
  'approved', 'view',
  '查看正式供应源', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_QUOTA', '供应配额', 'PAGE',
  '/supply-source/quotas', 'source/quota/index', 'source:quota:view',
  40, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:quota:view', 'source',
  'quota', 'view',
  '查看供应配额', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_CAPACITY', '能力与交期', 'PAGE',
  '/supply-source/capacities', 'source/capacity/index', 'source:capacity:view',
  50, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:capacity:view', 'source',
  'capacity', 'view',
  '查看能力与交期', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_RESTRICTION', '限制/暂停', 'PAGE',
  '/supply-source/restrictions', 'source/restriction/index', 'source:restriction:view',
  60, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:restriction:view', 'source',
  'restriction', 'view',
  '查看限制/暂停', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SOURCE_HISTORY', '供应源变更历史', 'PAGE',
  '/supply-source/history', 'source/history/index', 'source:history:view',
  70, 3, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SOURCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'source:history:view', 'source',
  'history', 'view',
  '查看供应源变更历史', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_REQUIREMENT', '采购需求', 'PAGE',
  '/procurement/requirements', 'procurement/requirement/index', 'procurement:requirement:view',
  10, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:requirement:view', 'procurement',
  'requirement', 'view',
  '查看采购需求', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_MATCHING', '供应源匹配', 'PAGE',
  '/procurement/matching', 'procurement/matching/index', 'procurement:matching:view',
  20, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:matching:view', 'procurement',
  'matching', 'view',
  '查看供应源匹配', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_GAP', '需求缺口', 'PAGE',
  '/procurement/gaps', 'procurement/gap/index', 'procurement:gap:view',
  30, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:gap:view', 'procurement',
  'gap', 'view',
  '查看需求缺口', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_ALLOCATION', '需求分配', 'PAGE',
  '/procurement/allocations', 'procurement/allocation/index', 'procurement:allocation:view',
  40, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:allocation:view', 'procurement',
  'allocation', 'view',
  '查看需求分配', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_PURCHASE_ORDER', '采购订单', 'PAGE',
  '/procurement/purchase-orders', 'procurement/purchase-order/index', 'procurement:purchase-order:view',
  50, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:purchase-order:view', 'procurement',
  'purchase-order', 'view',
  '查看采购订单', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_CONFIRMATION', '订单确认', 'PAGE',
  '/procurement/confirmations', 'procurement/confirmation/index', 'procurement:confirmation:view',
  60, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:confirmation:view', 'procurement',
  'confirmation', 'view',
  '查看订单确认', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_CHANGE_CANCEL', '订单变更/取消', 'PAGE',
  '/procurement/changes-cancellations', 'procurement/change-cancel/index', 'procurement:change-cancel:view',
  70, 6, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:change-cancel:view', 'procurement',
  'change-cancel', 'view',
  '查看订单变更/取消', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PROCUREMENT_FORECAST_PLAN', '预测与交付计划', 'PAGE',
  '/procurement/forecast-plans', 'procurement/forecast-plan/index', 'procurement:forecast-plan:view',
  80, 7, FALSE, FALSE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PROCUREMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'procurement:forecast-plan:view', 'procurement',
  'forecast-plan', 'view',
  '查看预测与交付计划', FALSE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_PLAN', '交付计划', 'PAGE',
  '/delivery/plans', 'delivery/plan/index', 'delivery:plan:view',
  10, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:plan:view', 'delivery',
  'plan', 'view',
  '查看交付计划', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_ASN', 'ASN', 'PAGE',
  '/delivery/asn', 'delivery/asn/index', 'delivery:asn:view',
  20, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:asn:view', 'delivery',
  'asn', 'view',
  '查看ASN', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_PACKAGING_LABEL', '包装与标签', 'PAGE',
  '/delivery/packaging-labels', 'delivery/packaging-label/index', 'delivery:packaging-label:view',
  30, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:packaging-label:view', 'delivery',
  'packaging-label', 'view',
  '查看包装与标签', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_SHIPMENT', '发运/在途', 'PAGE',
  '/delivery/shipments', 'delivery/shipment/index', 'delivery:shipment:view',
  40, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:shipment:view', 'delivery',
  'shipment', 'view',
  '查看发运/在途', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_APPOINTMENT', '到货预约', 'PAGE',
  '/delivery/appointments', 'delivery/appointment/index', 'delivery:appointment:view',
  50, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:appointment:view', 'delivery',
  'appointment', 'view',
  '查看到货预约', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_RECEIPT', '收货反馈', 'PAGE',
  '/delivery/receipts', 'delivery/receipt/index', 'delivery:receipt:view',
  60, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:receipt:view', 'delivery',
  'receipt', 'view',
  '查看收货反馈', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_QUANTITY_DIFFERENCE', '数量差异', 'PAGE',
  '/delivery/quantity-differences', 'delivery/quantity-difference/index', 'delivery:quantity-difference:view',
  70, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:quantity-difference:view', 'delivery',
  'quantity-difference', 'view',
  '查看数量差异', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_DELIVERY_RETURN', '退货发运', 'PAGE',
  '/delivery/returns', 'delivery/return/index', 'delivery:return:view',
  80, 7, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_DELIVERY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'delivery:return:view', 'delivery',
  'return', 'view',
  '查看退货发运', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_AUDIT', '审核计划', 'PAGE',
  '/quality/audits', 'quality/audit/index', 'quality:audit:view',
  10, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:audit:view', 'quality',
  'audit', 'view',
  '查看审核计划', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_MATERIAL_QUALIFICATION', '物料资格', 'PAGE',
  '/quality/material-qualifications', 'quality/material-qualification/index', 'quality:material-qualification:view',
  20, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:material-qualification:view', 'quality',
  'material-qualification', 'view',
  '查看物料资格', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_SAMPLE_TRIAL', '样件/试制', 'PAGE',
  '/quality/samples-trials', 'quality/sample-trial/index', 'quality:sample-trial:view',
  30, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:sample-trial:view', 'quality',
  'sample-trial', 'view',
  '查看样件/试制', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_PRODUCTION_APPROVAL', '量产认可', 'PAGE',
  '/quality/production-approvals', 'quality/production-approval/index', 'quality:production-approval:view',
  40, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:production-approval:view', 'quality',
  'production-approval', 'view',
  '查看量产认可', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_INSPECTION', '检验结果', 'PAGE',
  '/quality/inspections', 'quality/inspection/index', 'quality:inspection:view',
  50, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:inspection:view', 'quality',
  'inspection', 'view',
  '查看检验结果', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_ISSUE', '质量异常', 'PAGE',
  '/quality/issues', 'quality/issue/index', 'quality:issue:view',
  60, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:issue:view', 'quality',
  'issue', 'view',
  '查看质量异常', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_SCAR_8D', 'SCAR/8D', 'PAGE',
  '/quality/scar-8d', 'quality/scar-8d/index', 'quality:scar-8d:view',
  70, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:scar-8d:view', 'quality',
  'scar-8d', 'view',
  '查看SCAR/8D', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_REWORK_REINSPECTION', '退货/返工/复验', 'PAGE',
  '/quality/rework-reinspection', 'quality/rework-reinspection/index', 'quality:rework-reinspection:view',
  80, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:rework-reinspection:view', 'quality',
  'rework-reinspection', 'view',
  '查看退货/返工/复验', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_QUALITY_SUPPLIER_CHANGE', '供应商变更', 'PAGE',
  '/quality/supplier-changes', 'quality/supplier-change/index', 'quality:supplier-change:view',
  90, 8, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_QUALITY';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'quality:supplier-change:view', 'quality',
  'supplier-change', 'view',
  '查看供应商变更', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SETTLEMENT_RECONCILIATION', '对账单', 'PAGE',
  '/settlement/reconciliations', 'settlement/reconciliation/index', 'settlement:reconciliation:view',
  10, 9, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SETTLEMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'settlement:reconciliation:view', 'settlement',
  'reconciliation', 'view',
  '查看对账单', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SETTLEMENT_DIFFERENCE', '差异处理', 'PAGE',
  '/settlement/differences', 'settlement/difference/index', 'settlement:difference:view',
  20, 9, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SETTLEMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'settlement:difference:view', 'settlement',
  'difference', 'view',
  '查看差异处理', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SETTLEMENT_INVOICE', '发票协同', 'PAGE',
  '/settlement/invoices', 'settlement/invoice/index', 'settlement:invoice:view',
  30, 9, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SETTLEMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'settlement:invoice:view', 'settlement',
  'invoice', 'view',
  '查看发票协同', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SETTLEMENT_PAYMENT_REQUEST', '请款材料', 'PAGE',
  '/settlement/payment-requests', 'settlement/payment-request/index', 'settlement:payment-request:view',
  40, 9, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SETTLEMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'settlement:payment-request:view', 'settlement',
  'payment-request', 'view',
  '查看请款材料', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SETTLEMENT_PAYABLE_STATUS', '应付状态', 'PAGE',
  '/settlement/payable-status', 'settlement/payable-status/index', 'settlement:payable-status:view',
  50, 9, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SETTLEMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'settlement:payable-status:view', 'settlement',
  'payable-status', 'view',
  '查看应付状态', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SETTLEMENT_PAYMENT_RESULT', '付款结果', 'PAGE',
  '/settlement/payment-results', 'settlement/payment-result/index', 'settlement:payment-result:view',
  60, 9, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SETTLEMENT';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'settlement:payment-result:view', 'settlement',
  'payment-result', 'view',
  '查看付款结果', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_INDICATOR_MODEL', '指标模型', 'PAGE',
  '/performance/indicator-models', 'performance/indicator-model/index', 'performance:indicator-model:view',
  10, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:indicator-model:view', 'performance',
  'indicator-model', 'view',
  '查看指标模型', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_SCORE', '绩效评分', 'PAGE',
  '/performance/scores', 'performance/score/index', 'performance:score:view',
  20, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:score:view', 'performance',
  'score', 'view',
  '查看绩效评分', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_RATING', '评级结果', 'PAGE',
  '/performance/ratings', 'performance/rating/index', 'performance:rating:view',
  30, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:rating:view', 'performance',
  'rating', 'view',
  '查看评级结果', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_RISK', '风险台账', 'PAGE',
  '/performance/risks', 'performance/risk/index', 'performance:risk:view',
  40, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:risk:view', 'performance',
  'risk', 'view',
  '查看风险台账', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_ALERT', '预警事件', 'PAGE',
  '/performance/alerts', 'performance/alert/index', 'performance:alert:view',
  50, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:alert:view', 'performance',
  'alert', 'view',
  '查看预警事件', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_IMPROVEMENT', '改善计划', 'PAGE',
  '/performance/improvements', 'performance/improvement/index', 'performance:improvement:view',
  60, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:improvement:view', 'performance',
  'improvement', 'view',
  '查看改善计划', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_SUPPLY_DECISION', '供应决策', 'PAGE',
  '/performance/supply-decisions', 'performance/supply-decision/index', 'performance:supply-decision:view',
  70, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:supply-decision:view', 'performance',
  'supply-decision', 'view',
  '查看供应决策', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_PERFORMANCE_SECOND_SOURCE', '第二供应源计划', 'PAGE',
  '/performance/second-source-plans', 'performance/second-source/index', 'performance:second-source:view',
  80, 10, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_PERFORMANCE';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'performance:second-source:view', 'performance',
  'second-source', 'view',
  '查看第二供应源计划', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_ORGANIZATION', '企业组织', 'PAGE',
  '/master-data/organizations', 'masterdata/organization/index', 'masterdata:organization:view',
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:organization:view', 'masterdata',
  'organization', 'view',
  '查看企业组织', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_PURCHASING_ORG', '采购组织', 'PAGE',
  '/master-data/purchasing-organizations', 'masterdata/purchasing-organization/index', 'masterdata:purchasing-organization:view',
  20, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:purchasing-organization:view', 'masterdata',
  'purchasing-organization', 'view',
  '查看采购组织', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_PLANT_RECEIVING', '工厂/收货地点', 'PAGE',
  '/master-data/plants-receiving', 'masterdata/plant-receiving/index', 'masterdata:plant-receiving:view',
  30, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:plant-receiving:view', 'masterdata',
  'plant-receiving', 'view',
  '查看工厂/收货地点', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_MATERIAL', '物料', 'PAGE',
  '/master-data/materials', 'masterdata/material/index', 'masterdata:material:view',
  40, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:material:view', 'masterdata',
  'material', 'view',
  '查看物料', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_CATEGORY', '品类', 'PAGE',
  '/master-data/categories', 'masterdata/category/index', 'masterdata:category:view',
  50, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:category:view', 'masterdata',
  'category', 'view',
  '查看品类', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_UNIT_CURRENCY_TAX', '单位/币种/税码', 'PAGE',
  '/master-data/units-currencies-taxes', 'masterdata/unit-currency-tax/index', 'masterdata:unit-currency-tax:view',
  60, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:unit-currency-tax:view', 'masterdata',
  'unit-currency-tax', 'view',
  '查看单位/币种/税码', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_DELIVERY_LOCATION', '交付地点', 'PAGE',
  '/master-data/delivery-locations', 'masterdata/delivery-location/index', 'masterdata:delivery-location:view',
  70, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:delivery-location:view', 'masterdata',
  'delivery-location', 'view',
  '查看交付地点', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_MASTERDATA_EXTERNAL_MAPPING', '外围映射', 'PAGE',
  '/master-data/external-mappings', 'masterdata/external-mapping/index', 'masterdata:external-mapping:view',
  80, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_MASTERDATA';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'masterdata:external-mapping:view', 'masterdata',
  'external-mapping', 'view',
  '查看外围映射', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_USER', '用户', 'PAGE',
  '/system/users', 'system/user/index', 'system:user:view',
  10, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:user:view', 'system',
  'user', 'view',
  '查看用户', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_ROLE', '角色与挂载用户', 'PAGE',
  '/system/roles', 'system/role/index', 'system:role:view',
  20, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:role:view', 'system',
  'role', 'view',
  '查看角色与挂载用户', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_PERMISSION', '权限与数据域', 'PAGE',
  '/system/permissions', 'system/permission/index', 'system:permission:view',
  30, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:permission:view', 'system',
  'permission', 'view',
  '查看权限与数据域', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_WORKFLOW', '流程', 'PAGE',
  '/system/workflows', 'system/workflow/index', 'system:workflow:view',
  40, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:workflow:view', 'system',
  'workflow', 'view',
  '查看流程', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_DICTIONARY_PARAMETER', '字典参数', 'PAGE',
  '/system/dictionaries-parameters', 'system/dictionary-parameter/index', 'system:dictionary-parameter:view',
  50, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:dictionary-parameter:view', 'system',
  'dictionary-parameter', 'view',
  '查看字典参数', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_TEMPLATE_ATTACHMENT', '模板附件', 'PAGE',
  '/system/templates-attachments', 'system/template-attachment/index', 'system:template-attachment:view',
  60, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:template-attachment:view', 'system',
  'template-attachment', 'view',
  '查看模板附件', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_MESSAGE', '消息', 'PAGE',
  '/system/messages', 'system/message/index', 'system:message:view',
  70, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:message:view', 'system',
  'message', 'view',
  '查看消息', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_AUDIT_LOG', '审计日志', 'PAGE',
  '/system/audit-logs', 'system/audit-log/index', 'system:audit-log:view',
  80, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:audit-log:view', 'system',
  'audit-log', 'view',
  '查看审计日志', TRUE, 'flyway', 'flyway'
);

INSERT INTO sys_menu (
  parent_id, menu_code, label, menu_type, route, component_key, permission_code,
  sort_order, phase, visible, enabled, created_by, updated_by
) SELECT
  id, 'MENU_SYSTEM_INTEGRATION_JOB', '接口与任务监控', 'PAGE',
  '/system/integrations-jobs', 'system/integration-job/index', 'system:integration-job:view',
  90, 1, TRUE, TRUE, 'flyway', 'flyway'
FROM sys_menu WHERE menu_code = 'MENU_SYSTEM';

INSERT INTO sys_permission (
  permission_code, domain_code, resource_code, action_code, description,
  enabled, created_by, updated_by
) VALUES (
  'system:integration-job:view', 'system',
  'integration-job', 'view',
  '查看接口与任务监控', TRUE, 'flyway', 'flyway'
);

-- SUPER_ADMIN receives every enabled menu and permission.
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'SUPER_ADMIN' AND m.enabled = TRUE AND m.visible = TRUE;

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.enabled = TRUE;

-- SKELETON_VIEWER proves database menu filtering and backend 403 enforcement.
INSERT INTO sys_role_menu (role_id, menu_id, created_by)
SELECT r.id, m.id, 'flyway'
FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'SKELETON_VIEWER'
  AND m.menu_code IN ('MENU_WORKBENCH', 'MENU_WORKBENCH_HOME', 'MENU_SUPPLIER', 'MENU_SUPPLIER_POOL');

INSERT INTO sys_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'flyway'
FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'SKELETON_VIEWER'
  AND p.permission_code IN ('workbench:home:view', 'supplier:pool:view');

