package com.example.calorieserver.dto;

import com.example.calorieserver.entity.User;
import com.example.calorieserver.util.CalorieCalculator;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private User.Gender gender;
    private Integer age;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;

    // 目标
    private Double targetWeightKg;
    private Integer targetDays;

    // 评估
    private Boolean usedWeightLossDrug;
    private String diseases;
    private User.EatingHabit eatingHabit;
    private User.SocialEating socialEating;
    private User.HungerLevel hungerLevel;
    private Boolean hasBodyFatScale;

    // 体成分
    private Double bmi;
    private String bmiCategory;
    private Double bodyFat;
    private Double waistCm;
    private Double hipCm;
    private Double whr;

    // 系统计算
    private Double bmr;
    private Double tdee;
    private Double dailyCalorieTarget;
    private Integer recommendedDays;
    private Integer recommendedMeals;

    // 营养素
    private Double proteinRatio;
    private Double fatRatio;
    private Double carbsRatio;
    private Double proteinTargetG;
    private Double fatTargetG;
    private Double carbsTargetG;
    private Double proteinMaxG;
    private Double fatMaxG;
    private Double carbsMaxG;

    // 偏好
    private String allergies;

    private User.Role role;
    private LocalDateTime createdAt;

    // JWT（登录/注册时返回）
    private String token;

    public static UserResponse fromEntity(User user) {
        Double bmi = null;
        String bmiCategory = null;
        if (user.getHeightCm() != null && user.getWeightKg() != null
                && user.getHeightCm() > 0 && user.getWeightKg() > 0) {
            bmi = CalorieCalculator.calculateBMI(user);
            bmiCategory = CalorieCalculator.getBMICategory(user);
        }

        Integer age = null;
        if (user.getBirthDate() != null) {
            age = CalorieCalculator.calculateAge(user);
        }

        Double whr = CalorieCalculator.calculateWHR(user);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .gender(user.getGender())
                .age(age)
                .birthDate(user.getBirthDate())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .targetWeightKg(user.getTargetWeightKg())
                .targetDays(user.getTargetDays())
                .usedWeightLossDrug(user.getUsedWeightLossDrug())
                .diseases(user.getDiseases())
                .eatingHabit(user.getEatingHabit())
                .socialEating(user.getSocialEating())
                .hungerLevel(user.getHungerLevel())
                .hasBodyFatScale(user.getHasBodyFatScale())
                .bmi(bmi)
                .bmiCategory(bmiCategory)
                .bodyFat(user.getBodyFat())
                .waistCm(user.getWaistCm())
                .hipCm(user.getHipCm())
                .whr(whr)
                .bmr(user.getBmr())
                .tdee(user.getTdee())
                .dailyCalorieTarget(user.getDailyCalorieTarget())
                .recommendedDays(user.getRecommendedDays())
                .recommendedMeals(user.getRecommendedMeals())
                .proteinRatio(user.getProteinRatio())
                .fatRatio(user.getFatRatio())
                .carbsRatio(user.getCarbsRatio())
                .proteinTargetG(user.getProteinTargetG())
                .fatTargetG(user.getFatTargetG())
                .carbsTargetG(user.getCarbsTargetG())
                .proteinMaxG(user.getProteinMaxG())
                .fatMaxG(user.getFatMaxG())
                .carbsMaxG(user.getCarbsMaxG())
                .allergies(user.getAllergies())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}