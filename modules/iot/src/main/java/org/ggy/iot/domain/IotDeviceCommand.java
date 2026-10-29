package org.ggy.iot.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;
import java.util.Date;

/** IoT 设备指令。@ggy */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device_command")
public class IotDeviceCommand extends BaseEntity {
    @TableId private Long commandId;
    private Long deviceId;
    private String requestId;
    private String commandType;
    private String desiredLightState;
    private String commandStatus;
    private String failureReason;
    private Date publishedTime;
    private Date ackTime;
    private Date finishedTime;
    private Date timeoutTime;
}
