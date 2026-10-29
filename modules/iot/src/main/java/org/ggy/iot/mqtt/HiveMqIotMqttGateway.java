package org.ggy.iot.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ggy.iot.config.IotMqttProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * 基于 HiveMQ 客户端的 MQTT 生命周期网关。
 *
 * @ggy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HiveMqIotMqttGateway implements IotMqttGateway, SmartLifecycle {

    private final IotMqttProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile BiConsumer<String, byte[]> messageHandler = (topic, payload) -> { };
    private volatile Mqtt5AsyncClient client;

    @Override
    public void start() {
        if (!properties.isEnabled() || !running.compareAndSet(false, true)) {
            return;
        }
        URI uri = URI.create(properties.getBrokerUri());
        int port = uri.getPort() > 0 ? uri.getPort() : ("ssl".equalsIgnoreCase(uri.getScheme()) ? 8883 : 1883);
        var builder = MqttClient.builder().useMqttVersion5()
            .identifier(properties.getClientIdPrefix() + UUID.randomUUID().toString().replace("-", ""))
            .serverHost(uri.getHost()).serverPort(port);
        if ("ssl".equalsIgnoreCase(uri.getScheme()) || "mqtts".equalsIgnoreCase(uri.getScheme())) {
            builder.sslWithDefaultConfig();
        }
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            builder.simpleAuth().username(properties.getUsername())
                .password(properties.getPassword() == null ? new byte[0] : properties.getPassword().getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth();
        }
        client = builder.buildAsync();
        client.connectWith().cleanStart(true).send().whenComplete((ack, error) -> {
            if (error != null) {
                log.error("IoT MQTT 连接失败", error);
                return;
            }
            subscribe(properties.topic("devices/+/events/#"));
            subscribe(properties.topic("devices/+/presence"));
            log.info("IoT MQTT 已连接到 {}", properties.getBrokerUri());
        });
    }

    private void subscribe(String topic) {
        client.subscribeWith().topicFilter(topic).qos(MqttQos.AT_LEAST_ONCE).callback(publish -> {
            byte[] payload = publish.getPayloadAsBytes();
            if (payload != null && payload.length <= properties.getMaxPayloadBytes()) {
                messageHandler.accept(publish.getTopic().toString(), payload);
            } else {
                log.warn("丢弃超出限制的 IoT MQTT 报文，topic={}", publish.getTopic());
            }
        }).send().exceptionally(error -> {
            log.error("IoT MQTT 订阅失败，topic={}", topic, error);
            return null;
        });
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false) && client != null) {
            client.disconnect();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void publishCommand(String topic, byte[] payload) {
        if (!isConnected()) {
            throw new IllegalStateException("IoT MQTT 未连接");
        }
        client.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload(payload).send();
    }

    @Override
    public void setMessageHandler(BiConsumer<String, byte[]> handler) {
        this.messageHandler = handler == null ? (topic, payload) -> { } : handler;
    }

    @Override
    public boolean isConnected() {
        return running.get() && client != null;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
