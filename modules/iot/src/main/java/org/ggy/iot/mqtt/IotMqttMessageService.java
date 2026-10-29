package org.ggy.iot.mqtt;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ggy.common.json.utils.JsonUtils;
import org.ggy.iot.service.impl.IotCommandServiceImpl;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** 解析设备 MQTT 上行消息并驱动指令状态机。@ggy */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotMqttMessageService {
    private final IotMqttGateway gateway;
    private final IotCommandServiceImpl commandService;

    @PostConstruct
    public void register() { gateway.setMessageHandler(this::handle); }

    @SuppressWarnings("unchecked")
    private void handle(String topic, byte[] payload) {
        String[] parts = topic.split("/");
        if (parts.length < 5) { log.warn("忽略格式错误的 IoT Topic：{}", topic); return; }
        String deviceKey = parts[3];
        String raw = new String(payload, StandardCharsets.UTF_8);
        try {
            Map<String, Object> data = JsonUtils.parseObject(raw, Map.class);
            String requestId = text(data.get("requestId"));
            String messageId = text(data.get("messageId"));
            if (topic.endsWith("/events/ack")) {
                commandService.onAck(deviceKey, requestId, Boolean.parseBoolean(text(data.get("accepted"))), text(data.get("message")));
            } else if (topic.endsWith("/events/state")) {
                commandService.onState(deviceKey, requestId, messageId, text(data.get("lightState")), raw);
            } else if (topic.endsWith("/presence")) {
                commandService.onPresence(deviceKey, messageId, Boolean.parseBoolean(text(data.get("online"))), text(data.get("reason")), raw);
            }
        } catch (Exception e) {
            log.warn("解析 IoT MQTT 报文失败，topic={}，原因={}", topic, e.getMessage());
        }
    }
    private String text(Object value) { return value == null ? null : String.valueOf(value); }
}
