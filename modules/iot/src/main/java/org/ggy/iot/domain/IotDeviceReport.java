package org.ggy.iot.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/** IoT 设备上报原始记录。@ggy */
@Data
@TableName("iot_device_report")
public class IotDeviceReport {
    @TableId private Long reportId;
    private Long deviceId;
    private String requestId;
    private String messageId;
    private String reportType;
    private Date eventTime;
    private Date receivedTime;
    private String lightState;
    private String payload;
}
