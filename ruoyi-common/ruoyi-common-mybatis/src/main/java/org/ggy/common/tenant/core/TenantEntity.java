package org.ggy.common.tenant.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ggy.common.mybatis.core.domain.BaseEntity;

/**
 * 单租户兼容基类。
 *
 * @deprecated 多租户模块已移除；保留字段仅用于兼容既有数据库结构。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Deprecated(since = "5.6.2", forRemoval = true)
public class TenantEntity extends BaseEntity {

    private String tenantId;
}
