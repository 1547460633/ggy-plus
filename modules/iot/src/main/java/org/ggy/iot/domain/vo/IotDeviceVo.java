package org.ggy.iot.domain.vo;

import lombok.Data;
import io.github.linpeilie.annotations.AutoMapper;
import org.ggy.iot.domain.IotDevice;
import java.util.Date;

/** IoT 设备返回对象，不暴露设备密钥摘要。@ggy */
@Data
@AutoMapper(target = IotDevice.class)
public class IotDeviceVo {
    private Long deviceId; private String deviceKey; private String deviceName; private String productType;
    private String enabled; private String onlineStatus; private String lightState; private Date lastSeenTime;
    private Date lastReportTime; private String lastOfflineReason; private String remark; private Date createTime;
}
