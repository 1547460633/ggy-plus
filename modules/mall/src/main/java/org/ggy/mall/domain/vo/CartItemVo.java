package org.ggy.mall.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/** 购物车项展示对象。 */
@Data
public class CartItemVo {
    private Long id;
    private Long productId;
    private String name;
    private String coverUrl;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private Boolean available;
}
