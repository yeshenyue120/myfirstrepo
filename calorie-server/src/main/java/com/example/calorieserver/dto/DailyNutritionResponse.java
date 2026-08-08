package com.example.calorieserver.dto;

public record DailyNutritionResponse(
    // 实际摄入
    Double protein,
    Double fat,
    Double carbs,
    Double totalCalories,
    // 推荐摄入（当日有效目标 = 基础目标 + 当天运动消耗）
    Double proteinTarget,
    Double fatTarget,
    Double carbsTarget,
    // 最大摄入（维持热量 TDEE 水平 + 当天运动消耗）
    Double proteinMax,
    Double fatMax,
    Double carbsMax,
    // 热量目标
    Double calorieTarget,
    Double calorieMax
) {}
