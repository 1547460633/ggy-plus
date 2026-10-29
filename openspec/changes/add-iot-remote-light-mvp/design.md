# Design

## Context

见 `proposal.md` 的问题与范围。当前项目是以 `admin` 为启动入口的 Spring Boot 多模块应用；现有模块通过 `modules/pom.xml` 聚合并由 `admin/pom.xml` 显式依赖。`application.yml` 已扫描 `org.ggy.**.mapper` 与 `org.ggy.**.domain`，通用层已提供 Sa-Token、MyBatis-Plus、BaseEntity、统一返回、操作日志、分页和 Redisson。

当前没有 MQTT 客户端、设备身份体系或物联网数据表。设备消息是高频、外部不可信输入，必须将 Broker 接入与后台用户 API 分开，并让 MySQL 而非 retained MQTT 消息成为业务状态和审计依据。

## Goals / Non-Goals

**Goals:**

- 在单体应用内实现可水平扩展的 ESP32 MQTT 接入与远程开关灯闭环。
- 以预注册凭据、TLS、Topic ACL 和请求关联防止设备冒用、越权和重复执行。
- 以设备实际状态上报而非“发布成功”确认命令成功。
- 保持现有 Controller、Service、Mapper、Bo/Vo 分层与 Sa-Token 权限模型。

**Non-Goals:**

- 不实现设备自助注册、OTA、视频、多协议网关、移动端和自动化编排。
- 不把 Broker、设备固件或部署基础设施纳入本仓库实现；本变更只提供服务端配置契约和接入代码。
- 不在实施过程直接执行开发数据库 DDL/DML。

## Decisions

### 1. 模块和分层

新增 `modules/iot`，根包为 `org.ggy.iot`，并按现有业务模块组织：

```text
modules/iot
  controller/IotDeviceController.java
  controller/IotCommandController.java
  controller/internal/IotMqttAuthController.java
  service/IIotDeviceService.java
  service/IIotCommandService.java
  service/IIotMqttMessageService.java
  service/impl/...
  mapper/IotDeviceMapper.java
  mapper/IotDeviceCommandMapper.java
  mapper/IotDeviceReportMapper.java
  domain/, domain/bo/, domain/vo/
  config/IotMqttProperties.java
  mqtt/IotMqttGateway.java
  mqtt/HiveMqIotMqttGateway.java
```

- `modules/pom.xml` 加入 `iot`，根 `pom.xml` 管理 MQTT 客户端版本，`admin/pom.xml` 依赖 `iot`。
- `iot/pom.xml` 复用 `common-core`、`common-mybatis`、`common-web`、`common-satoken`、`common-log`、`common-idempotent`、`common-redis`，并新增 MQTT 客户端依赖。推荐 HiveMQ MQTT Client，以 MQTT 5、TLS 和自动重连支持为优先；客户端仅藏在 `IotMqttGateway` 后。
- REST 控制器只处理人类用户请求；MQTT 订阅回调只调用消息服务，不能反向绕过业务校验。

备选方案是以 EMQX HTTP Webhook 将上行 MQTT 消息转为 REST。该方案省去 Java MQTT 客户端但把消息消费、重试和幂等分散到 Broker 规则中，且下行仍需另一条发布路径，因此不选。

### 2. Broker、认证和访问边界

部署支持 TLS、HTTP Authentication 和 HTTP Authorization 的 EMQX Broker。设备不访问业务 REST；设备连接 Broker 时使用：

```text
clientId = ggy-iot-{deviceKey}
username = {deviceKey}
password = {deviceSecret}
```

- 设备创建或轮换时由服务端生成至少 256-bit 随机 `deviceSecret`，数据库只保存 BCrypt 哈希和凭据版本；明文仅在创建或轮换响应中出现一次。
- Broker 的认证/授权回调访问 `/iot/internal/mqtt/authenticate` 与 `/iot/internal/mqtt/authorize`。这两个接口使用 Broker 专用 HMAC Header 或 mTLS，并在网关层限制仅 Broker 可达；它们不接受 Sa-Token 用户 Token，也不加入公网白名单。
- 认证服务校验 `deviceKey`、客户端标识前缀、启停状态和 BCrypt 哈希。授权服务只授予设备自身 Topic 的最小 PUB/SUB 权限。
- 后端 MQTT 服务账号与设备账号分离，服务账号允许发布命令并以共享订阅消费所有设备上行事件。多实例时使用 `$share/ggy-iot/...` 避免所有实例重复处理同一消息；数据库唯一键仍是最终幂等防线。

不采用把 deviceSecret 放入 Topic、查询参数或普通日志的方案，因为这些位置会扩散凭据并无法安全轮换。

### 3. Topic、QoS 和 JSON 协议

首期 Topic 固定为：

| Topic | 发布者 | QoS | Retain | 说明 |
| --- | --- | --- | --- | --- |
| `iot/v1/devices/{deviceKey}/commands/down` | 平台 | 1 | 否 | 下行灯控命令 |
| `iot/v1/devices/{deviceKey}/events/ack` | 设备 | 1 | 否 | 命令接受或拒绝回执 |
| `iot/v1/devices/{deviceKey}/events/state` | 设备 | 1 | 否 | 心跳和灯状态上报 |
| `iot/v1/devices/{deviceKey}/presence` | 设备 | 1 | 是 | 上线与离线遗嘱 |

设备 ACL：仅订阅自己的 `commands/down`，仅发布自己的三个上行 Topic。下行命令不 retained，防止设备重连后执行陈旧指令。设备连接后立即发布 retained `{"online":true}`；遗嘱为 retained `{"online":false}`。平台还以 `last_seen_time` 和配置的心跳阈值兜底判离线。

下行命令：

```json
{
  "messageId": "uuid-or-ulid",
  "requestId": "client-provided-idempotency-key",
  "type": "light.set",
  "issuedAt": 1760200000000,
  "expiresAt": 1760200030000,
  "data": { "on": true }
}
```

ACK：`messageId`、`requestId`、`accepted`、`code`、`message`、`reportedAt`。状态：`messageId`、`reportedAt`、`properties.light.on`，若由命令触发则必须携带同一 `requestId`。平台从 Topic 推导设备身份，拒绝载荷设备身份与 Topic 不一致、必填字段缺失、类型不匹配或超过报文大小限制的消息。

### 4. 命令状态机与并发控制

```text
PENDING --> PUBLISHED --> ACKED --> SUCCEEDED
   |            |            |
   v            v            v
 FAILED       TIMEOUT      FAILED

PUBLISHED -- matching state report --> SUCCEEDED
```

- 控制请求先以事务锁定设备行，检查设备存在、启用、在线及该设备是否存在未终态命令。
- 请求以 `(device_id, request_id)` 唯一。重复请求返回既有命令；同一 `requestId` 但目标状态不同视为参数冲突。
- 离线、停用和并发冲突同样创建可追溯的终态失败命令并记录原因，但不向 Broker 发布。REST 响应返回该命令记录，使调用方可统一查询结果。
- 合法在线请求先持久化 `PENDING`，提交事务后由网关发布 QoS 1；发布确认后标记 `PUBLISHED`。发布失败、ACK 拒绝和超时均写入错误码与错误信息。
- 匹配 `requestId` 且目标灯状态一致的状态上报是成功的最终凭据；状态上报可早于 ACK 到达，因此允许从 `PUBLISHED` 直接转 `SUCCEEDED`。
- 定时任务扫描过期非终态命令，写入 `TIMEOUT`。ESP32 必须以 `requestId` 去重，重复命令只重发 ACK/状态，不重复 GPIO 操作。

不以 MQTT QoS 1 的 Broker ACK 表示灯已点亮：QoS 只表示 Broker 投递链路，不能证明设备执行结果。

### 5. 数据模型与数据权限

新增以下表，主键使用现有雪花 ID 策略，不设置自增：

| 表 | 关键字段 | 约束和索引 |
| --- | --- | --- |
| `iot_device` | `device_id`、`device_key`、`credential_hash`、`device_status`、`online_status`、`light_on`、各类 last time | `device_key` 唯一；`(device_status, online_status, last_seen_time)` 索引；含审计字段和 `del_flag` |
| `iot_device_command` | `command_id`、`command_no`、`device_id`、`request_id`、目标状态、状态、失败原因、各阶段时间 | `command_no` 唯一、`(device_id, request_id)` 唯一、`(device_id, command_status, expire_time)` 索引 |
| `iot_device_report` | `report_id`、`device_id`、`message_id`、`request_id`、`light_on`、JSON payload、接收时间 | `(device_id, message_id)` 唯一；`(device_id, received_time)` 与 `request_id` 索引 |

迁移脚本 `db/migration/20261012_add_iot_remote_light.sql` 创建表、索引及 IoT 权限菜单。设备使用逻辑删除；命令和上报保留为审计记录。第一期按后台共享设备池实施，不添加用户-设备授权关联，也不把通用部门数据权限强加给硬件；角色权限是访问边界。后续多租户/家庭归属需新增明确的设备授权表。

### 6. REST API 与安全约定

| 方法和路径 | 请求/响应 | 权限 | 审计与校验 |
| --- | --- | --- | --- |
| `POST /iot/devices` | `IotDeviceAddBo` -> `R<IotDeviceCredentialVo>` | `iot:device:add` | INSERT；名称、类型必填；不记录密钥响应 |
| `GET /iot/devices/list` | 查询 Bo + `PageQuery` -> `TableDataInfo<IotDeviceVo>` | `iot:device:list` | 分页参数与筛选校验 |
| `GET /iot/devices/{id}`、`/status` | -> `R<IotDeviceVo>` | `iot:device:query` | 不存在返回业务错误 |
| `PUT /iot/devices/{id}` | `IotDeviceUpdateBo` -> `R<Void>` | `iot:device:edit` | UPDATE；禁止直接修改密钥哈希 |
| `POST /iot/devices/{id}/credential/rotate` | -> `R<IotDeviceCredentialVo>` | `iot:device:credential` | UPDATE；不记录密钥响应 |
| `POST /iot/devices/{id}/commands/light` | `IotLightCommandBo` -> `R<IotDeviceCommandVo>` | `iot:device:control` | OTHER；`requestId`、目标状态必填，`@RepeatSubmit` |
| `GET /iot/commands/list`、`/{commandNo}` | 查询 Bo + 分页 / 详情 Vo | `iot:command:list/query` | 不泄露凭据或哈希 |

控制器沿用 `@Validated`、`@RequiredArgsConstructor`、`@SaCheckPermission`、`@Log`、`R<T>` 与 `TableDataInfo<T>`。密钥签发和轮换的 `@Log` 设置 `isSaveResponseData = false`。消息体和设备凭据不得写入 `sys_oper_log`。

### 7. 配置和运行时行为

在 `admin/src/main/resources/application.yml` 追加 `iot.mqtt` 配置：Broker URI、TLS 开关/信任库、服务账号（仅环境变量）、客户端 ID 前缀、Topic 前缀、心跳阈值、命令超时、最大报文大小及内部回调密钥。敏感值只从环境变量取值；不新增空 Redis 密码配置。

设备状态写库时使用 MyBatis-Plus Mapper；命令创建和状态转移使用 `@Transactional(rollbackFor = Exception.class)`，并利用行锁和唯一索引处理多实例竞争。可用 Redis/Redisson 做短期协作锁或缓存，但任何 Redis 丢失不得改变命令最终事实。

## Risks / Trade-offs

- [Broker 未配置 TLS/HTTP ACL] -> 上线前以部署清单验证匿名连接关闭、TLS、认证和 Topic ACL；应用侧不将网络隔离视为唯一保护。
- [QoS 1 会重复投递] -> 设备按 requestId 幂等，平台按 `(device_id, message_id)` 去重并以唯一索引兜底。
- [设备在发布后断线] -> 设备状态报告才确认成功；命令到期写入可查询的超时原因。
- [上报表持续增长] -> 首期建立时间索引并保留清理/归档扩展点；生产保留周期在部署时确定。
- [Broker 回调可被伪造] -> 限制网络来源，并同时验证 mTLS 或 HMAC，不接受用户 Sa-Token 代替 Broker 身份。

## Migration Plan

1. 部署或确认 MQTT Broker 的 TLS、设备认证、授权、遗嘱和服务账号配置，但不开放设备连接。
2. 发布数据库迁移 `20261012_add_iot_remote_light.sql`，创建新表、索引和 IoT 权限菜单；既有表不变，因此无历史数据回填。
3. 发布包含 `iot` 模块和 MQTT 配置的应用，先以服务账号建立 Broker 连接并监控订阅状态。
4. 由管理员注册测试设备，将一次性凭据写入 ESP32 安全配置后验证上线、上报、控制和超时路径。
5. 回滚时先停止 IoT 服务账号和 Broker 回调，再移除应用依赖与菜单；确认不再需要审计数据后，按 `iot_device_report`、`iot_device_command`、`iot_device` 顺序删除新表。回滚不修改既有业务数据。
