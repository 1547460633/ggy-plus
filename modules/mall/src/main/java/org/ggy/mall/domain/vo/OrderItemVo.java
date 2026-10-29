package org.ggy.mall.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/** 订单明细展示对象。 */
@Data
public class OrderItemVo {
    private Long productId;
    private String productName;
    private String coverUrl;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
}
