package org.ggy.iot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;

/**
 * IoT MQTT 连接配置。
 *
 * @ggy
 */
@Data
@Validated
@ConfigurationProperties("iot.mqtt")
public class IotMqttProperties {

    /** 是否启用 MQTT 客户端。 */
    private boolean enabled;
    /** Broker 地址，例如 ssl://mqtt.example.com:8883。 */
    @NotBlank(message = "iot.mqtt.broker-uri 不能为空")
    private String brokerUri;
    /** 服务端 MQTT 用户名。 */
    private String username;
    /** 服务端 MQTT 密码，仅允许通过环境变量注入。 */
    private String password;
    /** 客户端 ID 前缀。 */
    @NotBlank(message = "iot.mqtt.client-id-prefix 不能为空")
    private String clientIdPrefix = "ggy-iot-";
    /** Topic 根前缀。 */
    @NotBlank(message = "iot.mqtt.topic-prefix 不能为空")
    private String topicPrefix = "iot/v1";
    /** 设备心跳超时秒数。 */
    @Min(value = 10, message = "iot.mqtt.heartbeat-timeout-seconds 至少为 10")
    private long heartbeatTimeoutSeconds = 90;
    /** 命令回执超时秒数。 */
    @Min(value = 5, message = "iot.mqtt.command-timeout-seconds 至少为 5")
    private long commandTimeoutSeconds = 30;
    /** 入站报文最大字节数。 */
    @Min(value = 1024, message = "iot.mqtt.max-payload-bytes 至少为 1024")
    private int maxPayloadBytes = 65536;
    /** Broker HTTP 回调验签密钥，仅允许通过环境变量注入。 */
    private String internalCallbackSecret;

    /** MQTT 启用时，内部回调密钥必须由环境变量注入。 */
    @AssertTrue(message = "启用 MQTT 时 iot.mqtt.internal-callback-secret 不能为空")
    public boolean isInternalCallbackSecretValid() {
        return !enabled || (internalCallbackSecret != null && !internalCallbackSecret.isBlank());
    }

    public String topic(String suffix) {
        return topicPrefix + "/" + suffix;
    }
}
