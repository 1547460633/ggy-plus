# Spec Delta

## Purpose

定义 ESP32 与平台之间可验证、可约束且可恢复的 MQTT 连接和状态上报契约，使后台展示的设备在线及灯状态具有明确来源。

## ADDED Requirements

### Requirement: 设备使用专属凭据和受限 Topic 连接
系统 MUST 仅允许启用设备使用匹配的 `deviceKey`、`deviceSecret` 和客户端标识连接 MQTT Broker。已认证设备 SHALL 只能订阅自身命令 Topic，并只能发布自身确认、状态和在线 Topic；匿名、停用或凭据不匹配的连接 MUST 被拒绝。

#### Scenario: 合法设备连接
- **WHEN** 启用设备以匹配凭据和客户端标识连接 Broker
- **THEN** Broker 允许其访问该设备专属的受限 Topic 集合

#### Scenario: 凭据错误或设备已停用
- **WHEN** 设备使用错误密钥、已轮换旧密钥或已停用设备身份连接
- **THEN** Broker 拒绝连接且平台不更新设备在线状态

#### Scenario: 越权 Topic 操作
- **WHEN** 已认证设备尝试发布或订阅其他设备的 Topic
- **THEN** Broker 拒绝该 Topic 操作

### Requirement: 平台处理上线、离线和心跳事件
设备 SHALL 向 `iot/v1/devices/{deviceKey}/presence` 以 QoS 1 发布上线信息，并配置同 Topic、QoS 1、retained 的离线遗嘱。平台 MUST 根据合法上线、离线或心跳消息更新最近活跃时间与在线状态，并在超过配置心跳阈值后将设备视为离线。

#### Scenario: 设备正常上线
- **WHEN** 合法设备连接后发布 retained 上线消息
- **THEN** 平台将设备标记为在线并更新最后上线和活跃时间

#### Scenario: 非正常断开
- **WHEN** Broker 发布设备配置的离线遗嘱
- **THEN** 平台将设备标记为离线并记录最后离线时间

#### Scenario: 心跳超时
- **WHEN** 在线设备在配置心跳阈值内未产生合法活跃消息
- **THEN** 平台将该设备状态更新为离线

### Requirement: 平台接收并去重设备灯状态上报
设备 SHALL 向 `iot/v1/devices/{deviceKey}/events/state` 以 QoS 1 发布 JSON 状态消息，消息 MUST 包含 `messageId`、`reportedAt` 与 `properties.light.on`，并可包含 `requestId`。平台 MUST 校验 Topic 与载荷身份、按设备和 `messageId` 去重、保存原始上报及更新最近灯状态。

#### Scenario: 接收有效灯状态
- **WHEN** 已认证设备在自身状态 Topic 发布结构合法且未重复的灯状态消息
- **THEN** 平台保存状态上报并更新设备最近灯状态与活跃时间

#### Scenario: 收到重复状态消息
- **WHEN** 平台再次收到同一设备具有相同 `messageId` 的状态消息
- **THEN** 平台不重复创建上报记录或重复触发命令结果变更

#### Scenario: 收到非法状态消息
- **WHEN** 状态消息缺少必填字段、灯状态不是布尔值或 Topic 身份不匹配
- **THEN** 平台拒绝该消息且不更新设备当前状态
