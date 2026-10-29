package org.ggy.iot.domain.vo;
import lombok.Data;
import io.github.linpeilie.annotations.AutoMapper;
import org.ggy.iot.domain.IotDeviceCommand;
import java.util.Date;
/** IoT 指令返回对象。@ggy */
@Data
@AutoMapper(target = IotDeviceCommand.class)
public class IotDeviceCommandVo {
    private Long commandId; private Long deviceId; private String requestId; private String commandType;
    private String desiredLightState; private String commandStatus; private String failureReason;
    private Date publishedTime; private Date ackTime; private Date finishedTime; private Date timeoutTime; private Date createTime;
}
