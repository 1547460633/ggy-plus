package org.ggy.iot.mqtt;

import java.util.function.BiConsumer;

/**
 * MQTT 网关抽象，便于用测试实现替代真实 Broker。
 *
 * @ggy
 */
public interface IotMqttGateway {

    /** 发布 QoS 1 的设备命令。 */
    void publishCommand(String topic, byte[] payload);

    /** 注册入站报文处理器。 */
    void setMessageHandler(BiConsumer<String, byte[]> handler);

    /** MQTT 是否已建立连接。 */
    boolean isConnected();
}
