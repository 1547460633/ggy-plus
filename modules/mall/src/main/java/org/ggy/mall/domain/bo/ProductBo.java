package org.ggy.mall.domain.bo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** 商品维护请求。 */
@Data
public class ProductBo {
    private Long id;
    @NotBlank(message = "商品名称不能为空")
    private String name;
    private String coverUrl;
    private String description;
    @NotNull(message = "售价不能为空") @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;
    @NotNull(message = "库存不能为空") @PositiveOrZero(message = "库存不能小于 0")
    private Integer stock;
    @NotNull(message = "商品状态不能为空") @Min(0) @Max(1)
    private Integer status;
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort = 0;
}
