package com.example.calorieserver.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 时间工具：统一使用上海时区计算"今天"。
 * 数据库时区固定为 Asia/Shanghai，避免依赖部署机器 JVM 时区导致日期错位一天。
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    public static LocalDate today() {
        return LocalDate.now(SHANGHAI);
    }
}
