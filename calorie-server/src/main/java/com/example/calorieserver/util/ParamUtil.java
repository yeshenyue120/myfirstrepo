package com.example.calorieserver.util;

/**
 * 请求参数卫生工具：对分页 / 天数等无上限的参数做钳位，防止负数或超大值打崩查询。
 */
public final class ParamUtil {

    private ParamUtil() {
    }

    /** 趋势/报告天数上限：最多一年 */
    public static final int MAX_DAYS = 366;

    /** 食物库分页单页上限 */
    public static final int MAX_PAGE_SIZE = 200;

    public static int clampDays(int days) {
        return Math.min(MAX_DAYS, Math.max(1, days));
    }

    public static int clampPage(int page) {
        return Math.max(0, page);
    }

    public static int clampSize(int size) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, size));
    }
}
