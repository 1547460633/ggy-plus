-- 移除多租户模块对应的数据库结构。
-- 适用数据库：ggy_plus（MySQL 8）。请在代码完成多租户清理后执行。

DROP TABLE `sys_tenant_package`;
DROP TABLE `sys_tenant`;

ALTER TABLE `sys_config` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_dept` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_dict_data` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_dict_type` DROP INDEX `tenant_id`, DROP COLUMN `tenant_id`;
ALTER TABLE `sys_logininfor` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_notice` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_oper_log` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_oss` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_oss_config` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_post` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_role` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_social` DROP COLUMN `tenant_id`;
ALTER TABLE `sys_user` DROP COLUMN `tenant_id`;
ALTER TABLE `test_demo` DROP COLUMN `tenant_id`;
ALTER TABLE `test_tree` DROP COLUMN `tenant_id`;
