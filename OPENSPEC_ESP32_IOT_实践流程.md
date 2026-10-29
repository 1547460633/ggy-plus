# ESP32 物联网点灯功能的 OpenSpec 使用流程

本文用于在 ggy-plus 中规划和实现“ESP32 等设备接入、状态上报与远程点灯”功能。核心原则是：**先用合适的 OpenSpec 技能说明要达成什么，再让该技能完成对应阶段的工作**；需求未确认前不直接编写业务代码。

## 1. 整体流程

```text
需求探索
  -> 创建变更方案
  -> 评审和调整方案
  -> 按任务实施
  -> 验证并同步规格
  -> 归档变更
```

| 阶段 | 先描述的内容 | 使用的技能 | 技能完成的工作 |
| --- | --- | --- | --- |
| 需求探索 | 想解决的问题、第一期边界、待决定的技术问题 | OpenSpec Explore | 调研现有项目，梳理方案与风险；不写业务代码 |
| 生成方案 | 已确定的功能范围、非目标和验收要求 | OpenSpec Propose | 创建 proposal、design、specs、tasks 等变更文档 |
| 调整方案 | 需求、设计或任务的新增和变化 | OpenSpec Update Change | 协调更新已有变更文档；不写业务代码 |
| 编码实现 | 已批准的变更名称 | OpenSpec Apply Change | 按 tasks.md 实施、验证并汇总改动 |
| 同步规格 | 已实施且验证通过的能力 | OpenSpec Sync Specs | 将变更中的规格同步为长期规格 |
| 完成归档 | 已完成任务、已验证的变更 | OpenSpec Archive Change | 校验并归档变更，保留决策与实施历史 |

## 2. 第一阶段：先使用 OpenSpec Explore 澄清需求

### 先描述需要做什么

在输入框中选择 **OpenSpec Explore** 技能后，直接输入下面的内容。选中技能后，无需再额外写 `openspec explore:`。

```text
我要在 ggy-plus 新增 modules/iot 模块，用于接收和控制 ESP32 等物联网设备。

第一期实现“远程点灯”：设备预注册、设备在线状态、开灯、关灯、查询状态和设备上报状态。

请结合现有项目结构，帮我确定 MQTT 通信、设备认证、Topic 规范、命令状态机、数据库模型、权限及接口边界。

约束：沿用 Java 17、Spring Boot 3.5、MyBatis-Plus、Sa-Token 和现有 Controller/Service/Mapper 分层；不引入 org.dromara 内部包；不直接向开发数据库执行 DDL 或写入数据。
```

### 技能会做什么

OpenSpec Explore 只做调研和方案澄清，不创建业务代码。它应重点输出以下决策：

- 设备通信是否使用 MQTT，以及 MQTT Broker 的部署和接入边界；
- `deviceId`、`deviceKey`、`deviceSecret` 的设备身份与凭据轮换策略；
- 设备上线、离线、心跳、状态上报、服务端命令、设备回执的 Topic 与 JSON 报文；
- 命令的 `requestId`、超时、幂等、失败原因与状态机；
- 新模块位置、数据库表、REST 接口、Sa-Token 权限点与操作日志；
- 第一阶段不做的内容，例如 OTA、视频流、复杂自动化和移动端页面。

第一期推荐边界如下，最终以探索结论为准：

```text
管理端或 App
    -> ggy-plus 的 REST API
    -> modules/iot 通过 MQTT 下发命令和接收消息
    -> MQTT Broker
    -> ESP32 + LED
```

建议用 MQTT 处理设备长连接和消息推送，用 ggy-plus REST API 给管理端或 App 使用。

## 3. 第二阶段：使用 OpenSpec Propose 生成正式变更

### 先描述需要做什么

探索结论确认后，在输入框中选择 **OpenSpec Propose** 技能，并输入：

```text
为 ggy-plus 新增 modules/iot 物联网设备模块，第一期实现 ESP32 远程点灯 MVP。

范围：
- 新建 iot Maven 业务模块，并在 modules 和 admin 中完成聚合；
- 使用 MQTT 接收 ESP32 上报的上线、离线、心跳和灯状态；
- 提供设备预注册、设备分页、设备详情、开灯、关灯、设备状态查询和指令记录分页接口；
- 设备使用 deviceKey 和仅展示一次的 deviceSecret 认证；
- 命令和回执以 requestId 关联，设备离线或命令超时必须记录失败原因；
- 增加 Sa-Token 权限、操作日志、参数校验、统一返回和中文注释；
- 新增 MySQL 增量迁移脚本，并说明历史数据兼容与回滚；
- 明确 MQTT Topic、JSON 报文、QoS、遗嘱消息、主题 ACL 和幂等规则；
- 使用 Java 17 完成相关验证。

非目标：暂不实现 OTA、视频流、复杂自动化场景、多协议网关和移动端界面；不直接向开发数据库执行 DDL 或写入数据。
```

### 技能会做什么

OpenSpec Propose 会创建并填充类似下列目录的变更文档。当前项目中已存在同名示例变更：`openspec/changes/add-iot-remote-light-mvp/`。

```text
openspec/changes/add-iot-remote-light-mvp/
  proposal.md                 # 为什么做、范围、影响面和非目标
  design.md                   # 模块、接口、表、MQTT 与状态机设计
  specs/
    iot-device-management/spec.md
    iot-device-connectivity/spec.md
    iot-remote-light-control/spec.md
  tasks.md                    # 可实施的任务清单
```

其中规格必须用可验证的需求表达，而不是只描述实现方式。例如：

```text
Given 设备在线且当前灯为关闭状态
When 有权限的用户提交开灯命令
Then 服务端必须生成唯一 requestId 并发布 MQTT 命令
And 设备以相同 requestId 确认执行成功后
Then 服务端必须更新设备当前状态并保存指令执行结果
```

## 4. 第三阶段：评审后使用 OpenSpec Update Change 调整

### 先描述需要做什么

如果评审后发现需求变化，在输入框中选择 **OpenSpec Update Change**，再明确说明变更名和变化。例如：

```text
更新 OpenSpec 变更 add-iot-remote-light-mvp：
设备凭据需要支持轮换；增加亮度控制，但不增加 RGB 控制；
命令超时调整为 30 秒，并补充设备重复回执时的幂等要求。
请同步更新 proposal、design、相关 specs 和 tasks，暂不实现代码。
```

### 技能会做什么

该技能只修改变更方案中的文档，确保范围、技术设计、验收场景和任务清单保持一致。它不会直接改 Java 代码、配置、数据库或向 MQTT Broker 发布消息。

## 5. 第四阶段：使用 OpenSpec Apply Change 实施

### 先描述需要做什么

确认 proposal、design、specs 和 tasks 后，在输入框中选择 **OpenSpec Apply Change**，输入：

```text
实施 OpenSpec 变更 add-iot-remote-light-mvp。
严格按 tasks.md 完成 modules/iot、Maven 聚合、MQTT 接入、数据库迁移脚本、接口、权限、日志、校验和 Java 17 验证。
开始前先参考项目内相似模块；仅实现已批准的范围。
不要对开发数据库直接执行 DDL 或写入数据；只新增可执行的 db/migration 脚本并说明执行影响。
```

### 技能会做什么

该技能会按任务顺序实施，通常包括：

1. 新建 `modules/iot`，并在 `modules`、`admin` 等需要的 Maven 聚合位置接入。
2. 新增设备、设备状态、设备指令记录的 `domain`、`domain.bo`、`domain.vo`、Mapper、Service 和 Controller。
3. 新增 MQTT 配置、认证、订阅、发布、消息去重和命令超时处理。
4. 新增 MySQL 迁移脚本，例如 `db/migration/20261012_add_iot_remote_light.sql`。
5. 按现有风格补齐 Sa-Token 权限、`@Log`、`@Validated`、统一返回、中文注释和测试。
6. 使用 Java 17 执行与变更相关的编译或测试，并报告实际结果。

> 数据库迁移脚本可以随代码提交，但真正向开发数据库执行新增、修改、删除或 DDL，必须在执行前取得用户明确授权。

## 6. 第五阶段：验证、同步规格与归档

### 先描述需要做什么

实施完成后，依次使用以下技能：

```text
OpenSpec Sync Specs：
将 add-iot-remote-light-mvp 中已实现并验证通过的 delta specs 同步到主 specs，暂不归档。

OpenSpec Archive Change：
归档 add-iot-remote-light-mvp。归档前确认 tasks.md 全部完成、严格校验通过，并记录数据库迁移脚本和 Java 17 验证结论。
```

### 技能会做什么

- **OpenSpec Sync Specs**：把已确认的设备管理、设备连接和远程点灯规格沉淀到 `openspec/specs/`，使其成为项目的长期行为约束。
- **OpenSpec Archive Change**：校验任务与文档一致性，归档该次变更，保留方案、任务和实施历史，方便以后扩展传感器、调光或设备 OTA。

## 7. 使用提醒

- 小型 bug 修复或单一配置调整通常不需要 OpenSpec。
- 新模块、API、权限、MQTT 协议和数据库变更应优先走本流程。
- Explore、Propose、Update 用于澄清和文档；只有 Apply Change 才进入业务代码实施。
- 每次输入都要写清模块、范围、非目标、数据影响和验证要求，避免技能自行扩大范围。
