package org.ggy.mall.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

/** 用户购物车项。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_cart")
public class MallCart extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
