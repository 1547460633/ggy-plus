package org.ggy.common.tenant.helper;

import java.util.function.Supplier;

/**
 * 单租户兼容工具。
 *
 * @deprecated 多租户隔离已移除；所有动态租户调用均直接执行。
 */
@Deprecated(since = "5.6.2", forRemoval = true)
public final class TenantHelper {

    private TenantHelper() {
    }

    public static boolean isEnable() {
        return false;
    }

    public static String getTenantId() {
        return null;
    }

    public static void clearDynamic() {
    }

    public static void setDynamic(String tenantId, boolean global) {
    }

    public static void dynamic(String tenantId, Runnable handle) {
        handle.run();
    }

    public static <T> T dynamic(String tenantId, Supplier<T> handle) {
        return handle.get();
    }

    public static <T> T ignore(Supplier<T> handle) {
        return handle.get();
    }

    public static void ignore(Runnable handle) {
        handle.run();
    }
}
