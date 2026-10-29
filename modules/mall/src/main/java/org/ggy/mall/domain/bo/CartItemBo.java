package org.ggy.mall.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 加入或更新购物车请求。 */
@Data
public class CartItemBo {
    @NotNull(message = "商品不能为空")
    private Long productId;
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于 0")
    private Integer quantity;
}
