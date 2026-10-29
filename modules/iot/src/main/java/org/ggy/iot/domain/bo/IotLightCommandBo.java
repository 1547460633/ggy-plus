package org.ggy.iot.domain.bo;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 灯开关指令请求。@ggy */
@Data
public class IotLightCommandBo {
    @Pattern(regexp = "[01]", message = "lightState 只能为 0（关）或 1（开）")
    private String lightState;
}
