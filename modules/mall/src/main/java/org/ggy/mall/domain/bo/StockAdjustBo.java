package org.ggy.mall.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 人工库存调整请求。 */
@Data
public class StockAdjustBo {
    /**
     * 正数为入库、负数为出库；零值由业务层明确拦截。
     */
    @NotNull(message = "调整数量不能为空")
    private Integer changeQuantity;
    @NotBlank(message = "调整原因不能为空")
    private String remark;
}
