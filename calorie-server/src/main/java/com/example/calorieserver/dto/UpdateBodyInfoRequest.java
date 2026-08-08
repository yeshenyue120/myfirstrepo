package com.example.calorieserver.dto;

import com.example.calorieserver.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBodyInfoRequest {
    // P1-P6
    @NotNull(message = "请选择性别")
    private User.Gender gender;

    @NotNull(message = "请填写身高")
    private Double heightCm;

    @NotNull(message = "请填写体重")
    private Double weightKg;

    private Integer age;          // 前端传年龄，后端换算为出生年份
    private LocalDate birthDate;

    @NotNull(message = "请填写目标体重")
    private Double targetWeightKg;

    private Integer targetDays;

    // P9-P14
    private Boolean usedWeightLossDrug;
    private String diseases;
    private User.EatingHabit eatingHabit;
    private User.SocialEating socialEating;
    private User.HungerLevel hungerLevel;
    private Boolean hasBodyFatScale;

    // 体成分
    private Double bodyFat;
    private Double waistCm;
    private Double hipCm;

    // 偏好
    private String allergies;
}