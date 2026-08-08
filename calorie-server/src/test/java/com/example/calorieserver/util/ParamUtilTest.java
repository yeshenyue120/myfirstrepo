package com.example.calorieserver.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParamUtil 参数卫生工具测试：分页/天数钳位。
 */
class ParamUtilTest {

    @Test
    void clampDays_bounds() {
        assertEquals(1, ParamUtil.clampDays(0));
        assertEquals(1, ParamUtil.clampDays(-5));
        assertEquals(366, ParamUtil.clampDays(500));
        assertEquals(366, ParamUtil.clampDays(366));
    }

    @Test
    void clampDays_normal() {
        assertEquals(30, ParamUtil.clampDays(30));
        assertEquals(1, ParamUtil.clampDays(1));
    }

    @Test
    void clampPage_nonNegative() {
        assertEquals(0, ParamUtil.clampPage(-1));
        assertEquals(0, ParamUtil.clampPage(0));
        assertEquals(3, ParamUtil.clampPage(3));
    }

    @Test
    void clampSize_bounds() {
        assertEquals(1, ParamUtil.clampSize(0));
        assertEquals(1, ParamUtil.clampSize(-10));
        assertEquals(200, ParamUtil.clampSize(1000));
        assertEquals(200, ParamUtil.clampSize(200));
    }

    @Test
    void clampSize_normal() {
        assertEquals(50, ParamUtil.clampSize(50));
    }
}
