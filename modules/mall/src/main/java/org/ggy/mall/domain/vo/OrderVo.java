package org.ggy.mall.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** 订单展示对象。 */
@Data
public class OrderVo {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private Date createTime;
    private List<OrderItemVo> items;
}
