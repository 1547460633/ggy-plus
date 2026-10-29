package org.ggy.iot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT 模块配置入口。
 *
 * @ggy
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(IotMqttProperties.class)
public class IotMqttConfiguration {
}
