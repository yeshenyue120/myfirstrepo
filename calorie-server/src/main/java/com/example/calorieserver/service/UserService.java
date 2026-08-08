package com.example.calorieserver.service;

import com.example.calorieserver.dto.LoginRequest;
import com.example.calorieserver.dto.RegisterRequest;
import com.example.calorieserver.dto.UpdateBodyInfoRequest;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.UserRepository;
import com.example.calorieserver.security.JwtUtil;
import com.example.calorieserver.util.CalorieCalculator;
import com.example.calorieserver.util.TimeUtil;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 注册
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已被注册");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("该用户名已被占用");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser)
                .toBuilder()
                .token(jwtUtil.generateToken(savedUser.getId(), savedUser.getRole()))
                .build();
    }

    // 登录
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> userRepository.findByUsername(request.getEmail())
                        .orElseThrow(() -> new BusinessException("邮箱/用户名或密码错误")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("邮箱/用户名或密码错误");
        }

        return UserResponse.fromEntity(user)
                .toBuilder()
                .token(jwtUtil.generateToken(user.getId(), user.getRole()))
                .build();
    }

    // 根据邮箱查询
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserResponse.fromEntity(user);
    }

    // 14 步信息收集完成后一次性提交
    @Transactional
    public UserResponse submitOnboarding(Long userId, UpdateBodyInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // ===== P1-P6 基础数据 =====
        user.setGender(request.getGender());
        user.setHeightCm(request.getHeightCm());
        user.setWeightKg(request.getWeightKg());

        // 年龄换算：优先用 birthDate，否则用 age 反推（取当年 1 月 1 日作为近似）
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        } else if (request.getAge() != null) {
            int birthYear = TimeUtil.today().getYear() - request.getAge();
            user.setBirthDate(LocalDate.of(birthYear, 1, 1));
        }

        user.setTargetWeightKg(request.getTargetWeightKg());
        user.setTargetDays(request.getTargetDays());

        // ===== P9-P14 评估数据 =====
        user.setUsedWeightLossDrug(request.getUsedWeightLossDrug());
        user.setDiseases(request.getDiseases());
        user.setEatingHabit(request.getEatingHabit());
        user.setSocialEating(request.getSocialEating());
        user.setHungerLevel(request.getHungerLevel());
        user.setHasBodyFatScale(request.getHasBodyFatScale());

        // ===== 体成分 =====
        user.setBodyFat(request.getBodyFat());
        user.setWaistCm(request.getWaistCm());
        user.setHipCm(request.getHipCm());

        // ===== 偏好 =====
        user.setAllergies(request.getAllergies());

        // ===== 系统计算 =====
        double bmr = CalorieCalculator.calculateBMR(user);
        user.setBmr(Math.round(bmr * 10.0) / 10.0);

        double tdee = CalorieCalculator.calculateTDEE(user, bmr);
        user.setTdee(Math.round(tdee * 10.0) / 10.0);

        // 推荐天数
        int recommendedDays = CalorieCalculator.calculateRecommendedDays(user, tdee);
        user.setRecommendedDays(recommendedDays);

        // 如果用户没填目标天数，用推荐天数
        if (user.getTargetDays() == null || user.getTargetDays() <= 0) {
            user.setTargetDays(recommendedDays);
        }

        // 反算目标热量
        double targetCalories = CalorieCalculator.calculateCaloriesByDays(user, tdee, user.getTargetDays());
        user.setDailyCalorieTarget(targetCalories);

        // 营养素
        CalorieCalculator.assignMacroRatios(user);
        CalorieCalculator.calculateMacroTargets(user);
        // 最大摄入量（维持热量 TDEE 水平）
        CalorieCalculator.calculateMacroMax(user);

        // 推荐餐次
        user.setRecommendedMeals(CalorieCalculator.recommendMeals(user));

        // 体脂率估算
        if (user.getBodyFat() == null) {
            user.setBodyFat(CalorieCalculator.estimateBodyFat(user));
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    // 查询用户资料（供前端体重变化后同步最新目标）
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserResponse.fromEntity(user);
    }

    // 体重记录变化后：用最新体重临时重算目标，不覆盖 user.weightKg（起始体重，供仪表盘作起点）
    @Transactional
    public UserResponse recalculateTargetsFromWeight(Long userId, double currentWeightKg) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        double savedWeight = user.getWeightKg();
        user.setWeightKg(currentWeightKg);
        try {
            double bmr = CalorieCalculator.calculateBMR(user);
            user.setBmr(Math.round(bmr * 10.0) / 10.0);

            double tdee = CalorieCalculator.calculateTDEE(user, bmr);
            user.setTdee(Math.round(tdee * 10.0) / 10.0);

            user.setRecommendedDays(CalorieCalculator.calculateRecommendedDays(user, tdee));

            if (user.getTargetDays() != null && user.getTargetDays() > 0) {
                double target = CalorieCalculator.calculateCaloriesByDays(user, tdee, user.getTargetDays());
                user.setDailyCalorieTarget(Math.round(target * 10.0) / 10.0);
            }

            CalorieCalculator.assignMacroRatios(user);
            CalorieCalculator.calculateMacroTargets(user);
            CalorieCalculator.calculateMacroMax(user);
            user.setRecommendedMeals(CalorieCalculator.recommendMeals(user));
        } finally {
            user.setWeightKg(savedWeight);
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}