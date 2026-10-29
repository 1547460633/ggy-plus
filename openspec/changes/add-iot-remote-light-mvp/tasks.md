# Tasks

## 1. Maven 模块与运行配置

- [x] 1.1 在 `modules/iot/pom.xml` 建立 `org.ggy:iot` 模块，复用所需 common 依赖并加入受版本管理的 MQTT 客户端；同步修改 `modules/pom.xml`、根 `pom.xml` 和 `admin/pom.xml`，以 `mvn -pl modules/iot -am compile -DskipTests` 验证模块可解析和编译。
- [x] 1.2 新增 `IotMqttProperties` 与 `admin/src/main/resources/application.yml` 的 `iot.mqtt` 配置（Broker URI、TLS、服务账号环境变量、Topic 前缀、心跳阈值、命令超时、最大报文与内部回调密钥），并以 Spring Boot 配置绑定测试验证缺失必需项会失败且不新增空 Redis 密码。
- [ ] 1.3 实现 MQTT 生命周期网关 `IotMqttGateway` 与其 HiveMQ 客户端实现，建立服务账号重连、共享订阅和 QoS 1 发布；以嵌入式/测试 Broker 或可替换网关测试验证订阅、发布和断线重连行为。

## 2. 数据库迁移与权限初始化

- [x] 2.1 在 `db/migration/20261012_add_iot_remote_light.sql` 新建 `iot_device`、`iot_device_command`、`iot_device_report` 及唯一键、查询索引、审计字段和逻辑删除字段；以 MySQL 语法校验或隔离测试库执行验证脚本可重复运行，且不向开发库执行 DDL。
- [x] 2.2 在同一迁移脚本新增 IoT 菜单/按钮及 `iot:device:list/query/add/edit/credential/control`、`iot:command:list/query` 权限初始化数据；验证管理员角色获得菜单而既有菜单、角色和业务表不被修改。
- [x] 2.3 为迁移脚本附注历史兼容、部署顺序和按 report、command、device 顺序回滚的说明；人工审查确认无历史数据回填或对既有表的变更。

## 3. IoT 持久模型与设备管理

- [x] 3.1 在 `modules/iot/src/main/java/org/ggy/iot/domain`、`domain.bo`、`domain.vo` 创建设备、命令、状态上报实体及 Bo/Vo，使用 `BaseEntity`、`@AutoMapper`、中文 JavaDoc 和 `@ggy`；以单元测试验证映射结果不含密钥明文或哈希。
- [ ] 3.2 创建 `IotDeviceMapper`、`IotDeviceCommandMapper`、`IotDeviceReportMapper` 并继承 `BaseMapperPlus`，补充设备行锁和命令/上报查询；以 Mapper 集成测试验证 `(device_id, request_id)` 与 `(device_id, message_id)` 唯一约束和分页索引查询路径。
- [ ] 3.3 实现 `IIotDeviceService` 及实现类，完成预注册、BCrypt 凭据哈希、一次性密钥展示、轮换、启停、分页、详情与状态查询；以单元/集成测试验证旧凭据失效、停用设备不可连接/控制以及详情不泄露敏感字段。

## 4. MQTT 认证、状态接入与命令状态机

- [ ] 4.1 实现内部 Broker 认证和授权服务及 `/iot/internal/mqtt/authenticate`、`/authorize` 入口，校验 HMAC 或 mTLS、`deviceKey`、客户端标识、启停状态和 Topic ACL；以接口测试验证合法设备、错误/旧凭据、停用设备和跨设备 Topic 均得到正确结果。
- [ ] 4.2 实现 presence、ACK 和 state 消息解析，固定 `iot/v1/devices/{deviceKey}` Topic、QoS 1、retained 遗嘱与报文大小限制；以消息测试验证上线、离线、心跳超时、载荷/Topic 不匹配和非法 JSON 不更新状态。
- [ ] 4.3 实现状态上报持久化和 `(device_id, message_id)` 幂等处理，更新最近灯状态和活跃时间；以重复 MQTT 消息测试验证只保留一条上报且不会重复驱动命令状态。
- [ ] 4.4 实现灯控命令服务，使用事务与设备行锁创建 `PENDING`、按 `(device_id, request_id)` 幂等、限制单设备未终态命令，并对离线、停用、发布失败写入明确终态失败原因；以并发与重复 requestId 测试验证不重复下发 GPIO 指令。
- [ ] 4.5 实现 ACK/状态回执驱动的 `PUBLISHED`、`ACKED`、`SUCCEEDED`、`FAILED`、`TIMEOUT` 状态转换和过期扫描任务；以测试验证状态上报早于 ACK、设备拒绝、发布异常和执行超时均得到可查询结果。

## 5. REST 接口、日志与端到端验证

- [ ] 5.1 实现 `IotDeviceController` 的注册、分页、详情、状态、编辑和凭据轮换接口，使用 `@Validated`、Sa-Token 权限、`@Log`、`@RepeatSubmit`、`R<T>`/`TableDataInfo<T>` 和中文注释；以 MockMvc 测试验证正常、未授权、权限不足、参数错误和不存在设备路径。
- [ ] 5.2 实现 `IotCommandController` 的开灯、关灯、指令分页与详情接口，确保控制请求返回含 requestId、最终/进行中状态和失败原因的命令 Vo，且日志不保存设备密钥或 MQTT 报文；以 MockMvc 测试验证离线失败记录、幂等重试和权限边界。
- [x] 5.3 增加 MQTT 协议 JSON 示例与 Broker 部署清单文档，覆盖 TLS、匿名连接关闭、HTTP 认证/授权、Topic ACL、遗嘱和 ESP32 requestId 去重；以人工联调清单验证设备可上线、上报、开灯、关灯和超时失败。
- [x] 5.4 使用 Java 17 运行 `mvn -pl modules/iot -am test -DskipTests=false` 与 `mvn -pl admin -am package -DskipTests`，记录实际结果；若受外部 Broker 或测试环境限制未执行的验证，明确说明原因和替代验证。

## Workflow follow-up

- 在实现和审查完成后运行 OpenSpec 严格验证，并在确认所有任务完成后归档该变更。
