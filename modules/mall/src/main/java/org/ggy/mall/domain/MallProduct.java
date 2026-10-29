package org.ggy.mall.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

/** 商品。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_product")
public class MallProduct extends BaseEntity {
    @TableId
    private Long id;
    private String name;
    private String coverUrl;
    private String description;
    private BigDecimal price;
    private Integer stock;
    /** 0 上架，1 下架 */
    private Integer status;
    private Integer sort;
}
