# Proposal

## Why

当前 ggy-plus 没有物联网设备模块和 MQTT 接入能力，ESP32 等设备无法安全地向平台报告运行状态，也无法接受后台远程控制。第一期以远程点灯 MVP 建立设备身份、在线状态、状态上报与可追溯命令闭环，为后续扩展传感器和更多设备能力提供稳定边界。

## What Changes

- 新增 `modules/iot` Maven 业务模块，并在 `modules` 聚合模块及 `admin` 启动模块中接入。
- 新增设备预注册、凭据生成、设备分页、设备详情与运行状态查询能力；设备以 `deviceKey` 和仅展示一次的 `deviceSecret` 认证。
- 接入 MQTT，处理 ESP32 的上线、离线、心跳和灯状态上报，并将设备在线状态和最近上报状态持久化。
- 新增开灯、关灯及指令记录分页接口；以 `requestId` 关联命令、确认与状态回执，记录离线、拒绝、发布失败和执行超时原因。
- 定义首期 MQTT Topic、JSON 报文、QoS、遗嘱消息、主题 ACL、消息去重和命令状态机。
- 新增 Sa-Token 权限点、操作日志、参数校验、统一返回对象与中文注释。
- 新增 MySQL 增量迁移脚本 `db/migration/20261012_add_iot_remote_light.sql`，仅随部署执行，不向开发数据库直接写入数据或执行 DDL。

## Capabilities

### New Capabilities

- `iot-device-management`: 管理员预注册设备、签发和轮换设备凭据，并查询设备基础信息和当前运行状态。
- `iot-device-connectivity`: 设备经 MQTT 认证后受限访问所属 Topic，并将在线、离线、心跳和状态消息可靠、幂等地接入平台。
- `iot-remote-light-control`: 有权限的后台用户向在线设备提交开灯或关灯命令，并查询由 `requestId` 关联的最终命令结果与失败原因。

### Modified Capabilities

- 无。

## Impact

- Maven：修改 `modules/pom.xml`、根 `pom.xml` 的依赖管理（如新增 MQTT 客户端版本属性）及 `admin/pom.xml`；新增 `modules/iot/pom.xml`。
- 后端：新增 `org.ggy.iot` 下的 `controller`、`service`、`service.impl`、`mapper`、`domain`、`domain.bo`、`domain.vo`、MQTT 配置与消息处理组件；不引入 `org.dromara` 内部包。
- 接口：新增 `/iot/devices`、设备状态、灯控与指令记录相关 REST API，统一使用 `R<T>` 或 `TableDataInfo<T>`。
- 权限：新增 `iot:device:list/query/add/edit/credential`、`iot:device:control`、`iot:command:list/query` 等权限点和相应菜单/按钮数据。
- 配置与外部系统：增加 MQTT Broker 地址、TLS、服务账号、Topic 前缀、心跳阈值和命令超时配置；需要部署支持 HTTP 认证/授权和 TLS 的 MQTT Broker。
- 数据库：新增设备、设备命令、设备状态上报表与索引。既有业务表和历史数据不变；回滚按“上报记录、命令、设备”的依赖顺序删除新增表并移除菜单/配置，不影响现有模块。
- 验证：实施阶段以 Java 17 编译相关 Maven 模块，并覆盖 MQTT 消息幂等、命令超时、离线拒绝、权限不足及接口参数校验测试。
