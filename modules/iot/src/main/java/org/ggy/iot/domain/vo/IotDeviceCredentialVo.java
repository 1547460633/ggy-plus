package org.ggy.iot.domain.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
/** 仅在注册或轮换时返回一次的设备凭证。@ggy */
@Data @AllArgsConstructor
public class IotDeviceCredentialVo { private Long deviceId; private String deviceKey; private String deviceSecret; }
