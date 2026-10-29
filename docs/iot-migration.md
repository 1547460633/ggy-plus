# IoT 数据迁移说明

执行 [`db/migration/20261012_add_iot_remote_light.sql`](../db/migration/20261012_add_iot_remote_light.sql) 前，应完成备份并在隔离环境验证。脚本使用 `CREATE TABLE IF NOT EXISTS`、菜单 `ON DUPLICATE KEY UPDATE` 及角色菜单 `INSERT IGNORE`，可安全重复执行；它不会修改任何既有业务表、菜单或角色记录。

影响范围仅为新增的 `iot_device`、`iot_device_command`、`iot_device_report` 三张表以及菜单 ID `1700` 到 `1708`。设备密钥只存 BCrypt 摘要，现有用户密码和其他认证数据不受影响。

回滚时先删除管理员角色与上述菜单的关联，再删除菜单，最后按上报、指令、设备的逆序删除新增表。回滚将丢弃 IoT 设备和历史指令数据，执行前必须另行导出归档。

## 2026-10-14 开发库执行记录

已在 `ggy_plus`（MySQL 8.0.46）通过项目配置的 `mysql_ggy_dev` MCP 执行本迁移。首次执行成功创建三张表并新增 9 条 IoT 菜单、9 条管理员角色菜单关联；第二次执行同一脚本同样成功，确认脚本可重复执行。回查确认以下唯一索引存在：`iot_device(device_key)`、`iot_device_command(device_id, request_id)`、`iot_device_report(device_id, message_id)`。
