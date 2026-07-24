INSERT INTO sys_role (
  id, role_code, role_name, description, status, created_by, updated_by
) VALUES
  (100, 'SUPER_ADMIN', '超级管理员', '阶段0全菜单和全权限验收角色', 'ACTIVE', 'flyway', 'flyway'),
  (101, 'SKELETON_VIEWER', '骨架查看者', '阶段0有限只读菜单验收角色', 'ACTIVE', 'flyway', 'flyway');

