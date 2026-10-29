package org.ggy.mall.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/** 商城订单。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class MallOrder extends BaseEntity {
    @TableId
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    /** 0 待付款，1 待发货，2 已完成，3 已取消 */
    private Integer status;
    private Date cancelTime;
}
