package org.ggy.mall.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

/** 库存变动流水。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_stock_log")
public class MallStockLog extends BaseEntity {
    @TableId
    private Long id;
    private Long productId;
    private Integer changeQuantity;
    private Integer beforeStock;
    private Integer afterStock;
    private String businessType;
    private String businessNo;
    private String remark;
}
