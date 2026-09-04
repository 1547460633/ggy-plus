# ggy-plus 开发约束

## 项目背景

- 项目名称：ggy-plus
- Maven 坐标：`groupId = org.ggy`，根 `artifactId = ggy-plus`
- 基于 RuoYi-Vue-Plus 的多模块后端项目改造。
- 技术栈：Java 17、Spring Boot 3.5、MyBatis-Plus、Sa-Token、Redis/Redisson、MySQL、Lombok、MapStruct Plus、Springdoc。
- 根包名为 `org.ggy`。禁止新建或引用 `org.dromara` 的内部包。
- `org.dromara.sms4j`、`org.dromara.warm` 是第三方依赖包，必须保留。
- 主启动模块：`admin`。
- 业务模块：`modules`（system、job、generator、demo、workflow）。
- 公共能力：`common`。
- 扩展模块：`extend`。

## 实现规范

- 严格沿用仓库现有的目录结构、命名风格、注解和实现方式；优先参考同模块的相似功能。
- 业务实体放在 `domain`，请求对象放在 `domain.bo`，返回对象放在 `domain.vo`。
- 数据库访问使用 Mapper；业务逻辑放在 Service / ServiceImpl；接口层使用 Controller。
- 新增接口时，按项目现有风格补全权限注解、操作日志注解、参数校验、分页及统一返回结构。
- 优先使用项目已有的工具类、异常体系、枚举、常量、缓存和权限机制；不要重复造轮子。
- 涉及数据表或字段变更时，提供可执行的 MySQL SQL，并说明迁移影响。
- 不修改无关代码，不擅自调整第三方依赖版本，不删除既有功能。
- 代码注释使用中文；新增作者注释使用 `@ggy`。
- Redis 未设置密码时，不要保留空的 `password:` 配置项；应删除或注释该项，避免 Redisson 发送空密码认证。

## 验证要求

- 修改完成后，说明改动文件、核心实现和验证结果。
- 验证项目时使用 Java 17；项目不兼容 Java 8。

## 数据库 MCP

- 本项目已配置本地 MySQL MCP：`mysql_ggy_dev`，服务文件位于 `tools/mysql-mcp`，运行时位于 `tools/node`。
- 它连接开发数据库 `ggy_plus`，默认只允许只读 SQL（如 `SELECT`、`SHOW`、`DESCRIBE`、`EXPLAIN`）。
- 执行任何新增、修改、删除或 DDL SQL 前，必须先取得用户明确授权，再开启相应写入权限。
