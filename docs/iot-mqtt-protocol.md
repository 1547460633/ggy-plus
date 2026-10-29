# IoT MQTT 接入协议（远程点灯 MVP）

设备 Topic 使用 `iot/v1/devices/{deviceKey}` 前缀，`deviceKey` 仅允许服务端注册后下发的标识。

|方向|Topic|QoS|保留|
|---|---|---:|---:|
|平台→设备|`.../commands/down`|1|否|
|设备→平台|`.../events/ack`|1|否|
|设备→平台|`.../events/state`|1|否|
|设备→平台|`.../presence`|1|是|

平台命令：`{"requestId":"uuid","type":"LIGHT_SWITCH","lightState":"1","issuedAt":1760000000000}`。
设备 ACK：`{"requestId":"uuid","accepted":true,"message":"accepted","eventTime":1760000001000}`。
状态上报：`{"messageId":"uuid","requestId":"uuid","lightState":"1","eventTime":1760000002000}`；心跳可省略 `requestId`，但不得省略 `messageId`。
在线消息：`{"messageId":"uuid","online":true,"eventTime":1760000000000}`。设备连接时发布在线 retained 报文，并配置同 Topic、`{"messageId":"lwt-uuid","online":false}` 的 QoS 1 retained LWT。

每个 `requestId` 全局唯一，设备对重复 `requestId` 必须返回相同 ACK 且不得重复执行。平台每台设备同一时刻只允许一个 `PENDING`、`PUBLISHED` 或 `ACKED` 指令：PENDING→PUBLISHED→ACKED→SUCCEEDED；拒绝、离线、发送异常或超时进入 FAILED。状态报文仅在 `requestId` 与期望灯状态都匹配时确认成功。

Broker 采用 EMQX HTTP Auth/AuthZ，回调为 `POST /iot/internal/mqtt/authenticate` 和 `POST /iot/internal/mqtt/authorize`：认证请求携带 `clientid=deviceKey`、`username=deviceKey`、`password=deviceSecret`；服务端验证 BCrypt 摘要、设备启用状态。普通设备只允许发布自身 `events/#`、`presence`，只允许订阅自身 `commands/down`。内部 HTTP 回调必须附带 `X-Iot-Signature: HMAC-SHA256(body)`；回调网络还必须限制为 Broker 网段或 mTLS 入口。
