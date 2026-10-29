package org.ggy.iot.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.ggy.common.core.utils.MapstructUtils;
import org.ggy.iot.domain.IotDevice;
import org.ggy.iot.domain.vo.IotDeviceVo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

/** MQTT 必填配置校验。@ggy */
class IotMqttPropertiesTest {
    @Test
    void shouldRejectMissingRequiredProperties() {
        IotMqttProperties properties = new IotMqttProperties();
        properties.setBrokerUri(""); properties.setClientIdPrefix(""); properties.setTopicPrefix(""); properties.setInternalCallbackSecret("");
        assertFalse(Validation.buildDefaultValidatorFactory().getValidator().validate(properties).isEmpty());
    }

    @Test
    void shouldRequireCallbackSecretWhenMqttEnabled() {
        IotMqttProperties properties = new IotMqttProperties();
        properties.setEnabled(true); properties.setBrokerUri("ssl://mqtt.example.com:8883");
        properties.setClientIdPrefix("test-"); properties.setTopicPrefix("iot/v1"); properties.setInternalCallbackSecret("");
        assertFalse(Validation.buildDefaultValidatorFactory().getValidator().validate(properties).isEmpty());
    }

    @Test
    void shouldNotExposeSecretWhenMappingDeviceVo() {
        IotDevice device = new IotDevice();
        device.setDeviceSecretHash("$2a$10$never-expose-this-value");
        IotDeviceVo vo = MapstructUtils.convert(device, IotDeviceVo.class);
        assertNull(vo.getDeviceKey());
        assertTrue(Arrays.stream(IotDeviceVo.class.getDeclaredFields()).noneMatch(field -> field.getName().toLowerCase().contains("secret")));
    }
}
