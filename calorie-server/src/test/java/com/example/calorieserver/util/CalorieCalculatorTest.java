package com.example.calorieserver.util;

import com.example.calorieserver.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CalorieCalculator 核心计算引擎单元测试。
 * 覆盖：BMR / TDEE+活动系数 / BMI 分级 / 推荐天数 / 每日热量目标（缺口钳位、增重、安全下限）/ 营养素配比与克数 / 餐次 / 腰臀比 / 体脂率 / 年龄。
 */
class CalorieCalculatorTest {

    /** 固定 30 岁：birthDate = 今天 - 30 年，与运行时日期解耦，测试长期稳定 */
    private static final LocalDate AGE_30 = TimeUtil.today().minusYears(30);

    // ===== 测试用 User 构造 =====

    private User male(double weight, double height) {
        return User.builder()
                .gender(User.Gender.MALE)
                .weightKg(weight)
                .heightCm(height)
                .birthDate(AGE_30)
                .build();
    }

    private User female(double weight, double height) {
        return User.builder()
                .gender(User.Gender.FEMALE)
                .weightKg(weight)
                .heightCm(height)
                .birthDate(AGE_30)
                .build();
    }

    // ===== 年龄 =====

    @Test
    void calculateAge_noBirthDate_defaultsTo30() {
        assertEquals(30, CalorieCalculator.calculateAge(User.builder().build()));
    }

    @Test
    void calculateAge_birthDateToday_isZero() {
        User user = male(70, 175);
        user.setBirthDate(TimeUtil.today());
        assertEquals(0, CalorieCalculator.calculateAge(user));
    }

    @Test
    void calculateAge_30YearsAgo_is30() {
        assertEquals(30, CalorieCalculator.calculateAge(male(70, 175)));
    }

    // ===== BMR（Mifflin-St Jeor）=====

    @Test
    void calculateBMR_male() {
        // 10×70 + 6.25×175 − 5×30 + 5 = 1648.75 → 1648.8
        assertEquals(1648.8, CalorieCalculator.calculateBMR(male(70, 175)));
    }

    @Test
    void calculateBMR_female() {
        // 10×70 + 6.25×175 − 5×30 − 161 = 1482.75 → 1482.8
        assertEquals(1482.8, CalorieCalculator.calculateBMR(female(70, 175)));
    }

    // ===== TDEE（评估数据推算活动系数）=====

    @Test
    void calculateTDEE_baselineFactor1_2() {
        // 基准 1.2：1648.8×1.2 = 1978.56 → 1978.6
        assertEquals(1978.6, CalorieCalculator.calculateTDEE(male(70, 175), 1648.8));
    }

    @Test
    void calculateTDEE_socialOftens_adds0_15() {
        User user = male(70, 175);
        user.setSocialEating(User.SocialEating.OFTEN); // +0.15 → 1.35
        assertEquals(2225.9, CalorieCalculator.calculateTDEE(user, 1648.8)); // 1648.8×1.35
    }

    @Test
    void calculateTDEE_combination() {
        User user = male(70, 175);
        user.setSocialEating(User.SocialEating.OFTEN); // +0.15
        user.setHungerLevel(User.HungerLevel.OFTEN);   // +0.10
        user.setHasBodyFatScale(true);                 // +0.05
        assertEquals(2473.2, CalorieCalculator.calculateTDEE(user, 1648.8)); // 1.5×1648.8
    }

    // ===== BMI =====

    @Test
    void calculateBMI() {
        // 65 / 1.7² = 22.49 → 22.5
        assertEquals(22.5, CalorieCalculator.calculateBMI(male(65, 170)));
    }

    @Test
    void calculateBMI_missingData_returnsZero() {
        assertEquals(0.0, CalorieCalculator.calculateBMI(User.builder().build()));
    }

    @Test
    void getBMICategory_boundaries() {
        assertEquals("偏瘦", CalorieCalculator.getBMICategory(male(53.0, 170))); // ≈18.3
        assertEquals("理想", CalorieCalculator.getBMICategory(male(53.5, 170))); // ≈18.5
        assertEquals("理想", CalorieCalculator.getBMICategory(male(69.0, 170))); // ≈23.9
        assertEquals("偏胖", CalorieCalculator.getBMICategory(male(69.4, 170))); // ≈24.0
        assertEquals("偏胖", CalorieCalculator.getBMICategory(male(80.7, 170))); // ≈27.9
        assertEquals("肥胖", CalorieCalculator.getBMICategory(male(81.0, 170))); // ≈28.0
    }

    // ===== 目标方向 & 需减/增公斤数 =====

    @Test
    void isWeightLoss_trueWhenTargetBelowWeight() {
        User user = male(70, 175);
        user.setTargetWeightKg(60.0);
        assertTrue(CalorieCalculator.isWeightLoss(user));
    }

    @Test
    void isWeightLoss_falseWhenTargetAboveWeight() {
        User user = male(70, 175);
        user.setTargetWeightKg(80.0);
        assertFalse(CalorieCalculator.isWeightLoss(user));
    }

    @Test
    void getWeightDelta() {
        User user = male(70, 175);
        user.setTargetWeightKg(60.0);
        assertEquals(10.0, CalorieCalculator.getWeightDelta(user));
    }

    @Test
    void getWeightDelta_noTarget_isZero() {
        assertEquals(0.0, CalorieCalculator.getWeightDelta(male(70, 175)));
    }

    // ===== 推荐天数 =====

    @Test
    void calculateRecommendedDays_normal15Percent() {
        User user = male(70, 175);
        user.setTargetWeightKg(65.0); // delta 5kg
        // 5×7700 / (2000×0.15) = 38500/300 ≈ 128.3 → 128
        assertEquals(128, CalorieCalculator.calculateRecommendedDays(user, 2000.0));
    }

    @Test
    void calculateRecommendedDays_conservative10Percent() {
        User user = male(70, 175);
        user.setTargetWeightKg(65.0);
        user.setEatingHabit(User.EatingHabit.HARD); // 保守对象 → 10%
        // 38500 / (2000×0.10) = 192.5 → 193
        assertEquals(193, CalorieCalculator.calculateRecommendedDays(user, 2000.0));
    }

    @Test
    void calculateRecommendedDays_noDelta_zero() {
        assertEquals(0, CalorieCalculator.calculateRecommendedDays(male(70, 175), 2000.0));
    }

    // ===== 每日热量目标（百分比缺口模式）=====

    @Test
    void calculateCaloriesByDays_lossWithDeficitCap() {
        User user = male(70, 175);
        user.setTargetWeightKg(60.0); // delta 10kg
        // required% = 10×7700/100/2000 = 38.5% → 钳到上限 25% → 2000×0.75 = 1500
        assertEquals(1500.0, CalorieCalculator.calculateCaloriesByDays(user, 2000.0, 100));
    }

    @Test
    void calculateCaloriesByDays_lossWithDeficitFloor() {
        User user = male(70, 175);
        user.setTargetWeightKg(69.0); // delta 1kg
        // required% = 1×7700/100/2000 = 3.85% → 钳到下限 10% → 2000×0.9 = 1800
        assertEquals(1800.0, CalorieCalculator.calculateCaloriesByDays(user, 2000.0, 100));
    }

    @Test
    void calculateCaloriesByDays_gain() {
        User user = male(70, 175);
        user.setTargetWeightKg(80.0); // delta 10kg
        // 盈余 = min(2000×20%, 500) = 400 → 2400
        assertEquals(2400.0, CalorieCalculator.calculateCaloriesByDays(user, 2000.0, 100));
    }

    @Test
    void calculateCaloriesByDays_gainSurplusCapped500() {
        User user = male(70, 175);
        user.setTargetWeightKg(80.0);
        // 盈余 = min(3000×20%, 500) = 500 → 3500
        assertEquals(3500.0, CalorieCalculator.calculateCaloriesByDays(user, 3000.0, 100));
    }

    @Test
    void calculateCaloriesByDays_safetyFloor_female() {
        User user = female(70, 175);
        user.setTargetWeightKg(60.0);
        // 2000×0.25 缺口 → 目标 900 → 钳到女性下限 1200
        assertEquals(1200.0, CalorieCalculator.calculateCaloriesByDays(user, 1200.0, 30));
    }

    @Test
    void calculateCaloriesByDays_noDelta_returnsTdee() {
        assertEquals(2000.0, CalorieCalculator.calculateCaloriesByDays(male(70, 175), 2000.0, 100));
    }

    // ===== 营养素配比 =====

    @Test
    void assignMacroRatios_lossDefault() {
        User user = male(70, 175);
        user.setTargetWeightKg(60.0);
        CalorieCalculator.assignMacroRatios(user);
        assertEquals(0.30, user.getProteinRatio());
        assertEquals(0.25, user.getFatRatio());
        assertEquals(0.45, user.getCarbsRatio());
    }

    @Test
    void assignMacroRatios_lossEasilyHungry() {
        User user = male(70, 175);
        user.setTargetWeightKg(60.0);
        user.setHungerLevel(User.HungerLevel.OFTEN);
        CalorieCalculator.assignMacroRatios(user);
        assertEquals(0.40, user.getProteinRatio());
        assertEquals(0.25, user.getFatRatio());
        assertEquals(0.35, user.getCarbsRatio());
    }

    @Test
    void assignMacroRatios_lossDiabetes() {
        User user = male(70, 175);
        user.setTargetWeightKg(60.0);
        user.setDiseases("糖尿病");
        CalorieCalculator.assignMacroRatios(user);
        assertEquals(0.35, user.getProteinRatio());
        assertEquals(0.30, user.getFatRatio());
        assertEquals(0.35, user.getCarbsRatio());
    }

    @Test
    void assignMacroRatios_gain() {
        User user = male(70, 175);
        user.setTargetWeightKg(80.0);
        CalorieCalculator.assignMacroRatios(user);
        assertEquals(0.25, user.getProteinRatio());
        assertEquals(0.25, user.getFatRatio());
        assertEquals(0.50, user.getCarbsRatio());
    }

    @Test
    void assignMacroRatios_maintain() {
        User user = male(70, 175); // 无目标 → 不增减 → 默认配比
        CalorieCalculator.assignMacroRatios(user);
        assertEquals(0.25, user.getProteinRatio());
        assertEquals(0.25, user.getFatRatio());
        assertEquals(0.50, user.getCarbsRatio());
    }

    // ===== 营养素克数 =====

    @Test
    void calculateMacroTargets() {
        User user = male(70, 175);
        user.setDailyCalorieTarget(2000.0);
        user.setProteinRatio(0.30);
        user.setFatRatio(0.25);
        user.setCarbsRatio(0.45);
        CalorieCalculator.calculateMacroTargets(user);
        assertEquals(150.0, user.getProteinTargetG()); // 2000×0.3/4
        assertEquals(55.6, user.getFatTargetG());      // 2000×0.25/9 ≈ 55.56 → 55.6
        assertEquals(225.0, user.getCarbsTargetG());   // 2000×0.45/4
    }

    @Test
    void calculateMacroTargets_zeroCalories_skips() {
        User user = male(70, 175);
        user.setDailyCalorieTarget(0.0);
        user.setProteinRatio(0.30);
        CalorieCalculator.calculateMacroTargets(user);
        assertNull(user.getProteinTargetG());
    }

    @Test
    void macroGrams_nullReturnsNull() {
        assertNull(CalorieCalculator.macroGrams(null, 0.3, 4));
        assertNull(CalorieCalculator.macroGrams(2000.0, null, 4));
    }

    @Test
    void calculateMacroMax() {
        User user = male(70, 175);
        user.setTdee(2400.0);
        user.setProteinRatio(0.30);
        user.setFatRatio(0.25);
        user.setCarbsRatio(0.45);
        CalorieCalculator.calculateMacroMax(user);
        assertEquals(180.0, user.getProteinMaxG()); // 2400×0.3/4
        assertEquals(66.7, user.getFatMaxG());      // 2400×0.25/9 ≈ 66.67 → 66.7
        assertEquals(270.0, user.getCarbsMaxG());   // 2400×0.45/4
    }

    // ===== 餐次推荐 =====

    @Test
    void recommendMeals() {
        assertEquals(3, CalorieCalculator.recommendMeals(male(70, 175)));

        User hard = male(70, 175);
        hard.setEatingHabit(User.EatingHabit.HARD);
        assertEquals(5, CalorieCalculator.recommendMeals(hard));

        User hungryOften = male(70, 175);
        hungryOften.setHungerLevel(User.HungerLevel.OFTEN);
        assertEquals(5, CalorieCalculator.recommendMeals(hungryOften));

        User hungrySometimes = male(70, 175);
        hungrySometimes.setHungerLevel(User.HungerLevel.SOMETIMES);
        assertEquals(4, CalorieCalculator.recommendMeals(hungrySometimes));
    }

    // ===== 体脂率估算 =====

    @Test
    void estimateBodyFat_male() {
        // BMI 22.5、age 30：1.2×22.5 + 0.23×30 − 16.2 = 17.7
        assertEquals(17.7, CalorieCalculator.estimateBodyFat(male(65, 170)));
    }

    @Test
    void estimateBodyFat_female() {
        // 1.2×22.5 + 0.23×30 − 5.4 = 28.5
        assertEquals(28.5, CalorieCalculator.estimateBodyFat(female(65, 170)));
    }

    // ===== 腰臀比 =====

    @Test
    void calculateWHR() {
        User user = male(70, 175);
        user.setWaistCm(80.0);
        user.setHipCm(100.0);
        assertEquals(0.8, CalorieCalculator.calculateWHR(user));
    }

    @Test
    void calculateWHR_missingReturnsNull() {
        User user = male(70, 175);
        assertNull(CalorieCalculator.calculateWHR(user));

        user.setWaistCm(80.0);
        assertNull(CalorieCalculator.calculateWHR(user)); // hip 缺

        user.setHipCm(0.0);
        assertNull(CalorieCalculator.calculateWHR(user)); // hip 为 0
    }
}
