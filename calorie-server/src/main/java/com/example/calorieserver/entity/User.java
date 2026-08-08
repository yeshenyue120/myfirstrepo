package com.example.calorieserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private String password;

    // ===== P1-P6 基础数据 =====
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private Double heightCm;

    private Double weightKg;

    private LocalDate birthDate;

    private Double targetWeightKg;

    private Integer targetDays;

    // ===== P9-P14 减重评估 =====
    private Boolean usedWeightLossDrug;

    private String diseases;  // 逗号分隔，如 "糖尿病,高血压"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EatingHabit eatingHabit;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SocialEating socialEating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HungerLevel hungerLevel;

    private Boolean hasBodyFatScale;

    // ===== 体成分（选填，后面加）=====
    private Double bodyFat;
    private Double waistCm;
    private Double hipCm;

    // ===== 饮食偏好（选填，后面加）=====
    private String allergies;

    // ===== 系统计算 =====
    private Double bmr;
    private Double tdee;
    private Double dailyCalorieTarget;
    private Double proteinRatio;
    private Double fatRatio;
    private Double carbsRatio;
    private Double proteinTargetG;
    private Double fatTargetG;
    private Double carbsTargetG;
    private Double proteinMaxG;
    private Double fatMaxG;
    private Double carbsMaxG;
    private Integer recommendedDays;
    private Integer recommendedMeals;

    // ===== 角色 =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== 枚举 =====
    public enum Gender { MALE, FEMALE }

    public enum EatingHabit { REGULAR, ADJUSTABLE, HARD }

    public enum SocialEating { RARE, SOMETIMES, OFTEN }

    public enum HungerLevel { RARE, SOMETIMES, OFTEN }

    public enum Role { USER, NUTRITIONIST, ADMIN }
}