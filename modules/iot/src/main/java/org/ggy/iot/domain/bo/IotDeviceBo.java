package org.ggy.iot.domain.bo;

import jakarta.validation.constraints.NotBlank;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.ggy.iot.domain.IotDevice;

/** IoT 设备新增或修改请求。@ggy */
@Data
@AutoMapper(target = IotDevice.class, reverseConvertGenerate = false)
public class IotDeviceBo {
    private Long deviceId;
    @NotBlank(message = "设备名称不能为空") @Size(max = 100, message = "设备名称长度不能超过100")
    private String deviceName;
    @Size(max = 32, message = "产品类型长度不能超过32") private String productType = "ESP32_LIGHT";
    private String enabled = "0";
    @Size(max = 500, message = "备注长度不能超过500") private String remark;
}
