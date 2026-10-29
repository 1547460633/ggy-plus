package org.ggy.iot.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ggy.iot.config.IotMqttProperties;
import org.ggy.iot.domain.IotDevice;
import org.ggy.iot.mapper.IotDeviceMapper;
import org.ggy.common.json.utils.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** EMQX HTTP Auth/AuthZ 内部回调，除 HMAC 外还应由入口网络策略限制来源。@ggy */
@SaIgnore
@RestController
@ConditionalOnProperty(prefix = "iot.mqtt", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/iot/internal/mqtt")
public class IotMqttInternalController {
    private static final BCryptPasswordEncoder SECRET_ENCODER = new BCryptPasswordEncoder();
    private final IotMqttProperties properties;
    private final IotDeviceMapper deviceMapper;

    @PostMapping({"/authenticate", "/auth"})
    public ResponseEntity<Map<String, String>> auth(@RequestHeader(value = "X-Iot-Signature", required = false) String signature, @RequestBody String body) {
        if (!validSignature(signature, body)) return deny();
        Map<String, Object> input = JsonUtils.parseObject(body, Map.class);
        String deviceKey = value(input, "username", value(input, "clientid", null));
        String password = value(input, "password", null);
        IotDevice device = deviceKey == null ? null : deviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getDeviceKey, deviceKey));
        boolean allow = device != null && "0".equals(device.getEnabled()) && password != null && SECRET_ENCODER.matches(password, device.getDeviceSecretHash());
        return allow ? ResponseEntity.ok(Map.of("result", "allow")) : deny();
    }

    @PostMapping({"/authorize", "/acl"})
    public ResponseEntity<Map<String, String>> acl(@RequestHeader(value = "X-Iot-Signature", required = false) String signature, @RequestBody String body) {
        if (!validSignature(signature, body)) return deny();
        Map<String, Object> input = JsonUtils.parseObject(body, Map.class);
        String deviceKey = value(input, "username", value(input, "clientid", null));
        String topic = value(input, "topic", null); String action = value(input, "action", "");
        String base = properties.topic("devices/" + deviceKey + "/");
        boolean allow = deviceKey != null && topic != null && (("publish".equalsIgnoreCase(action) && (topic.startsWith(base + "events/") || topic.equals(base + "presence")))
            || ("subscribe".equalsIgnoreCase(action) && topic.equals(base + "commands/down")));
        return allow ? ResponseEntity.ok(Map.of("result", "allow")) : deny();
    }

    private ResponseEntity<Map<String, String>> deny() { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", "deny")); }
    private String value(Map<String, Object> map, String key, String fallback) { Object value = map.get(key); return value == null ? fallback : String.valueOf(value); }
    private boolean validSignature(String signature, String body) {
        if (signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(properties.getInternalCallbackSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = hex(signature); return supplied != null && MessageDigest.isEqual(expected, supplied);
        } catch (Exception ignored) { return false; }
    }
    private byte[] hex(String value) {
        if ((value.length() & 1) == 1) return null; byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) { int part = Character.digit(value.charAt(i * 2), 16) * 16 + Character.digit(value.charAt(i * 2 + 1), 16); if (part < 0) return null; result[i] = (byte) part; }
        return result;
    }
}
