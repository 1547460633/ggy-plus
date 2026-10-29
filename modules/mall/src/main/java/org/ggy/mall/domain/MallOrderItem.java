package org.ggy.mall.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

/** 订单商品快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order_item")
public class MallOrderItem extends BaseEntity {
    @TableId
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String coverUrl;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
}
