package com.example.calorieserver.util;

import com.example.calorieserver.entity.User;
import java.time.LocalDate;
import java.time.Period;

public class CalorieCalculator {

    // ===== 年龄计算 =====
    public static int calculateAge(User user) {
        if (user.getBirthDate() == null) return 30;
        return Period.between(user.getBirthDate(), TimeUtil.today()).getYears();
    }

    // ===== BMR（Mifflin-St Jeor）=====
    public static double calculateBMR(User user) {
        int age = calculateAge(user);
        double bmr;
        if (user.getGender() == User.Gender.MALE) {
            bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * age + 5;
        } else {
            bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * age - 161;
        }
        return Math.round(bmr * 10.0) / 10.0;
    }

    // ===== TDEE（基于减重评估推算活动系数）=====
    public static double calculateTDEE(User user, double bmr) {
        double factor = estimateActivityFactor(user);
        return Math.round(bmr * factor * 10.0) / 10.0;
    }

    // ===== 根据评估数据估算活动系数（替代 activityLevel）=====
    private static double estimateActivityFactor(User user) {
        double factor = 1.2; // 久坐基准

        // 应酬多 → 活动量可能略高
        if (user.getSocialEating() == User.SocialEating.OFTEN) factor += 0.15;
        else if (user.getSocialEating() == User.SocialEating.SOMETIMES) factor += 0.075;

        // 容易饿 → 代谢可能偏高
        if (user.getHungerLevel() == User.HungerLevel.OFTEN) factor += 0.1;
        else if (user.getHungerLevel() == User.HungerLevel.SOMETIMES) factor += 0.05;

        // 有体脂秤 → 说明有一定健康意识，活动略多
        if (user.getHasBodyFatScale() != null && user.getHasBodyFatScale()) factor += 0.05;

        return Math.min(factor, 1.725); // 不超过高强度上限
    }

    // ===== BMI =====
    public static double calculateBMI(User user) {
        if (user.getHeightCm() == null || user.getWeightKg() == null) return 0;
        double heightM = user.getHeightCm() / 100.0;
        return Math.round(user.getWeightKg() / (heightM * heightM) * 10.0) / 10.0;
    }

    // ===== BMI 颜色分级 =====
    public static String getBMICategory(User user) {
        double bmi = calculateBMI(user);
        if (bmi < 18.5) return "偏瘦";
        else if (bmi < 24) return "理想";
        else if (bmi < 28) return "偏胖";
        else return "肥胖";
    }

    // ===== 判断目标方向 =====
    public static boolean isWeightLoss(User user) {
        return user.getTargetWeightKg() != null && user.getTargetWeightKg() < user.getWeightKg();
    }

    // ===== 需减/增公斤数 =====
    public static double getWeightDelta(User user) {
        if (user.getTargetWeightKg() == null) return 0;
        return Math.abs(user.getWeightKg() - user.getTargetWeightKg());
    }

    // ===== 推荐天数（基于 TDEE 百分比缺口）=====
    public static int calculateRecommendedDays(User user, double tdee) {
        double delta = getWeightDelta(user);
        if (delta == 0) return 0;

        // 默认 15% TDEE 缺口（温和可持续），保守情况降至 10%
        double percent = isConservativeCase(user) ? 0.10 : 0.15;

        double dailyDeficit = tdee * percent;
        int days = (int) Math.round(delta * 7700 / dailyDeficit);
        return Math.max(days, 14);
    }

    // ===== 判断是否为保守减脂对象 =====
    private static boolean isConservativeCase(User user) {
        if (user.getUsedWeightLossDrug() != null && user.getUsedWeightLossDrug()) return true;
        if (user.getDiseases() != null && user.getDiseases().contains("甲减")) return true;
        if (user.getEatingHabit() == User.EatingHabit.HARD) return true;
        if (user.getSocialEating() == User.SocialEating.OFTEN) return true;
        if (user.getHungerLevel() == User.HungerLevel.OFTEN) return true;
        return false;
    }

    // ===== 根据用户选的天数按百分比缺口计算每日热量目标（薄荷方式）=====
    public static double calculateCaloriesByDays(User user, double tdee, int targetDays) {
        double delta = getWeightDelta(user);
        if (delta == 0 || targetDays <= 0) return Math.round(tdee * 10.0) / 10.0;

        boolean losing = isWeightLoss(user);
        double minCalories = user.getGender() == User.Gender.MALE ? 1500 : 1200;

        // 达到目标每天需要的缺口占 TDEE 的比例
        double requiredPercent = (delta * 7700.0) / targetDays / tdee;

        double target;
        if (losing) {
            // 缺口比例控制在 10%~25% TDEE（温和～激进）
            double deficitPercent = Math.max(0.10, Math.min(0.25, requiredPercent));
            target = tdee * (1 - deficitPercent);
        } else {
            // 增重：日盈余 = TDEE 的 10%~20%，不超过 500 千卡
            double surplusPercent = Math.max(0.10, Math.min(0.20, requiredPercent));
            double surplus = Math.min(tdee * surplusPercent, 500);
            target = tdee + surplus;
        }

        // 安全钳位
        target = Math.max(minCalories, Math.min(target, tdee + 500));

        return Math.round(target * 10.0) / 10.0;
    }

    // ===== 腰臀比 =====
    public static Double calculateWHR(User user) {
        if (user.getWaistCm() == null || user.getHipCm() == null || user.getHipCm() == 0) return null;
        return Math.round(user.getWaistCm() / user.getHipCm() * 100.0) / 100.0;
    }

    // ===== 体脂率估算 =====
    public static double estimateBodyFat(User user) {
        double bmi = calculateBMI(user);
        int age = calculateAge(user);
        if (user.getGender() == User.Gender.MALE) {
            return Math.round((1.20 * bmi + 0.23 * age - 16.2) * 10.0) / 10.0;
        } else {
            return Math.round((1.20 * bmi + 0.23 * age - 5.4) * 10.0) / 10.0;
        }
    }

    // ===== 分配营养素比例（评估驱动）=====
    public static void assignMacroRatios(User user) {
        double protein = 0.25, fat = 0.25, carbs = 0.50;

        if (isWeightLoss(user)) {
            protein = 0.30;
            fat = 0.25;
            carbs = 0.45;

            // 容易饿 → 提高蛋白质
            if (user.getHungerLevel() == User.HungerLevel.OFTEN) {
                protein = 0.40;
                carbs = 0.35;
            } else if (user.getHungerLevel() == User.HungerLevel.SOMETIMES) {
                protein = 0.35;
                carbs = 0.40;
            }

            // 糖尿病 → 低碳水
            if (user.getDiseases() != null && user.getDiseases().contains("糖尿病")) {
                carbs = 0.35;
                protein = 0.35;
                fat = 0.30;
            }
        }

        // 增肌 → 高碳水
        if (!isWeightLoss(user) && getWeightDelta(user) > 0) {
            protein = 0.25;
            fat = 0.25;
            carbs = 0.50;
        }

        user.setProteinRatio(protein);
        user.setFatRatio(fat);
        user.setCarbsRatio(carbs);
    }

    // ===== 计算营养素克数 =====
    public static void calculateMacroTargets(User user) {
        Double calories = user.getDailyCalorieTarget();
        if (calories == null || calories == 0) return;
        user.setProteinTargetG(macroGrams(calories, user.getProteinRatio(), 4));
        user.setFatTargetG(macroGrams(calories, user.getFatRatio(), 9));
        user.setCarbsTargetG(macroGrams(calories, user.getCarbsRatio(), 4));
    }

    // ===== 营养素克数换算（推荐/最大共用）=====
    public static Double macroGrams(Double calories, Double ratio, double calPerGram) {
        if (calories == null || ratio == null) return null;
        return Math.round(calories * ratio / calPerGram * 10.0) / 10.0;
    }

    // ===== 计算最大摄入量（维持热量 TDEE 水平）=====
    public static void calculateMacroMax(User user) {
        Double tdee = user.getTdee();
        if (tdee == null || tdee <= 0) return;
        user.setProteinMaxG(macroGrams(tdee, user.getProteinRatio(), 4));
        user.setFatMaxG(macroGrams(tdee, user.getFatRatio(), 9));
        user.setCarbsMaxG(macroGrams(tdee, user.getCarbsRatio(), 4));
    }

    // ===== 推荐餐次 =====


    public static int recommendMeals(User user) {
        if (user.getEatingHabit() == User.EatingHabit.HARD) return 5;
        if (user.getHungerLevel() == User.HungerLevel.OFTEN) return 5;
        if (user.getHungerLevel() == User.HungerLevel.SOMETIMES) return 4;
        return 3;
    }
}