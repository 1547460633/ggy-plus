package org.ggy.iot.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/** IoT 设备。@ggy */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device")
public class IotDevice extends BaseEntity {
    @TableId private Long deviceId;
    private String deviceKey;
    private String deviceSecretHash;
    private String deviceName;
    private String productType;
    private String enabled;
    private String onlineStatus;
    private String lightState;
    private Date lastSeenTime;
    private Date lastReportTime;
    private String lastOfflineReason;
    private String remark;
    @TableLogic private String delFlag;
}
