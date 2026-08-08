package com.example.calorieserver.service;

import com.example.calorieserver.dto.MealRecordResponse;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.entity.*;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final UserRepository userRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final MealRecordRepository mealRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final WaterRecordRepository waterRecordRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final FoodRepository foodRepository;

    /**
     * 导出用户全部数据（readOnly 事务避免懒加载异常）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        List<MealRecord> mealRecords = mealRecordRepository.findByUserIdAndMealDateBetween(
                userId, java.time.LocalDate.of(2000, 1, 1), java.time.LocalDate.of(2100, 12, 31));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", UserResponse.fromEntity(user));
        result.put("weightRecords", weightRecordRepository.findByUserIdOrderByRecordedDateDescIdDesc(userId));
        result.put("mealRecords", mealRecords.stream().map(MealRecordResponse::fromEntity).toList());
        result.put("exerciseRecords", exerciseRecordRepository.findByUserIdAndRecordDateBetween(
                userId, java.time.LocalDate.of(2000, 1, 1), java.time.LocalDate.of(2100, 12, 31)));
        result.put("waterRecords", waterRecordRepository.findByUserIdAndDateBetween(
                userId, java.time.LocalDate.of(2000, 1, 1), java.time.LocalDate.of(2100, 12, 31)));
        result.put("sleepRecords", sleepRecordRepository.findByUserIdAndDateBetween(
                userId, java.time.LocalDate.of(2000, 1, 1), java.time.LocalDate.of(2100, 12, 31)));
        result.put("favorites", userFavoriteRepository.findByUserId(userId));
        result.put("customFoods", foodRepository.findByCreatorId(userId));
        return result;
    }
}
