package com.example.calorieserver.service;

import com.example.calorieserver.dto.DailyMealResponse;
import com.example.calorieserver.dto.DailyNutritionResponse;
import com.example.calorieserver.dto.MealRecordRequest;
import com.example.calorieserver.dto.MealRecordResponse;
import com.example.calorieserver.entity.Food;
import com.example.calorieserver.entity.MealRecord;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.ExerciseRecordRepository;
import com.example.calorieserver.repository.FoodRepository;
import com.example.calorieserver.repository.MealRecordRepository;
import com.example.calorieserver.repository.UserRepository;
import com.example.calorieserver.security.SecurityUtil;
import com.example.calorieserver.util.CalorieCalculator;
import com.example.calorieserver.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealRecordService {

    // 运动消耗计入可吃额度的折扣系数（运动消耗 × 0.9 计为有效额度，避免全额抵扣）
    private static final double EXERCISE_CREDIT_RATIO = 0.9;

    private final MealRecordRepository mealRecordRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;

    // 批量添加饮食记录
    @Transactional
    public List<MealRecordResponse> batchAddRecords(Long userId, List<MealRecordRequest> requests) {
        // 一次批量加载所有食物，避免逐条 findById（N 次查询）
        List<Long> foodIds = requests.stream().map(MealRecordRequest::getFoodId).distinct().toList();
        Map<Long, Food> foodMap = foodRepository.findAllById(foodIds).stream()
                .collect(Collectors.toMap(Food::getId, f -> f));

        List<MealRecord> records = requests.stream().map(req -> {
            Food food = foodMap.get(req.getFoodId());
            if (food == null) {
                throw new BusinessException("食物不存在: " + req.getFoodId());
            }

            double totalCalories = req.getGrams() / 100.0 * food.getCaloriesPer100g();

            return MealRecord.builder()
                    .user(User.builder().id(userId).build())
                    .food(food)
                    .grams(req.getGrams())
                    .totalCalories(Math.round(totalCalories * 10.0) / 10.0)
                    .mealType(req.getMealType())
                    .mealDate(req.getMealDate())
                    .note(req.getNote())
                    .build();
        }).toList();

        List<MealRecord> saved = mealRecordRepository.saveAll(records);
        return saved.stream().map(MealRecordResponse::fromEntity).toList();
    }

    // 添加饮食记录
    @Transactional
    public MealRecordResponse addRecord(Long userId, MealRecordRequest request) {
        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new BusinessException("食物不存在"));

        // 自动计算热量
        double totalCalories = request.getGrams() / 100.0 * food.getCaloriesPer100g();

        MealRecord record = MealRecord.builder()
                .user(User.builder().id(userId).build())
                .food(food)
                .grams(request.getGrams())
                .totalCalories(Math.round(totalCalories * 10.0) / 10.0)
                .mealType(request.getMealType())
                .mealDate(request.getMealDate())
                .note(request.getNote())
                .build();

        MealRecord saved = mealRecordRepository.save(record);
        return MealRecordResponse.fromEntity(saved);
    }

    // 查某天所有记录
    public List<MealRecordResponse> getRecordsByDate(Long userId, LocalDate date) {
        return mealRecordRepository.findByUserIdAndMealDate(userId, date)
                .stream()
                .map(MealRecordResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 查某天某一餐
    public List<MealRecordResponse> getRecordsByDateAndMeal(Long userId, LocalDate date, MealRecord.MealType mealType) {
        return mealRecordRepository.findByUserIdAndMealDateAndMealType(userId, date, mealType)
                .stream()
                .map(MealRecordResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 查昨天某一餐（用于"复制上一餐"）
    public List<MealRecordResponse> getYesterdayMeal(Long userId, String mealType) {
        LocalDate yesterday = TimeUtil.today().minusDays(1);
        MealRecord.MealType type = MealRecord.MealType.valueOf(mealType.toUpperCase());
        return getRecordsByDateAndMeal(userId, yesterday, type);
    }

    // 修改记录（改克数后自动重算热量）
    @Transactional
    public MealRecordResponse updateRecord(Long recordId, Double newGrams) {
        MealRecord record = mealRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());

        record.setGrams(newGrams);
        double totalCalories = newGrams / 100.0 * record.getFood().getCaloriesPer100g();
        record.setTotalCalories(Math.round(totalCalories * 10.0) / 10.0);

        MealRecord saved = mealRecordRepository.save(record);
        return MealRecordResponse.fromEntity(saved);
    }

    // 删除单条记录
    @Transactional
    public void deleteRecord(Long recordId) {
        MealRecord record = mealRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        mealRecordRepository.delete(record);
    }

    // 清空某天所有记录
    @Transactional
    public void deleteRecordsByDate(Long userId, LocalDate date) {
        mealRecordRepository.deleteByUserIdAndMealDate(userId, date);
    }

    // 统计某天总热量（命名避免误导：接口按传入 date 统计，不限于"今日"）
    public Double getTotalCalories(Long userId, LocalDate date) {
        return mealRecordRepository.sumCaloriesByUserIdAndMealDate(userId, date)
                .orElse(0.0);
    }

    // 近 N 天趋势
    public Map<LocalDate, Double> getWeeklyTrend(Long userId, LocalDate endDate, int days) {
        LocalDate startDate = endDate.minusDays(days - 1);
        List<Object[]> results = mealRecordRepository.sumCaloriesGroupByDate(userId, startDate, endDate);

        Map<LocalDate, Double> trend = new LinkedHashMap<>();
        for (Object[] row : results) {
            trend.put((LocalDate) row[0], (Double) row[1]);
        }
        return trend;
    }

    // 每日营养汇总（蛋白质/脂肪/碳水 + 推荐/最大摄入量，联动当天运动消耗）
    public DailyNutritionResponse getDailyNutrition(Long userId, LocalDate date) {
        List<MealRecord> records = mealRecordRepository.findByUserIdAndMealDate(userId, date);
        User user = userRepository.findById(userId).orElse(null);

        double protein = 0, fat = 0, carbs = 0, calories = 0;
        for (MealRecord r : records) {
            Food food = r.getFood();
            double ratio = r.getGrams() / 100.0;
            protein += ratio * (food.getProteinPer100g() != null ? food.getProteinPer100g() : 0);
            fat += ratio * (food.getFatPer100g() != null ? food.getFatPer100g() : 0);
            carbs += ratio * (food.getCarbsPer100g() != null ? food.getCarbsPer100g() : 0);
            calories += r.getTotalCalories();
        }

        // 当天运动消耗 → 计入有效热量预算（推荐=目标+运动，最大=TDEE+运动）
        Double exerciseCalories = exerciseRecordRepository.sumCaloriesByUserIdAndDate(userId, date);
        if (exerciseCalories == null) exerciseCalories = 0.0;

        Double calorieTarget = null, calorieMax = null;
        Double proteinTarget = null, fatTarget = null, carbsTarget = null;
        Double proteinMax = null, fatMax = null, carbsMax = null;

        if (user != null) {
            // 推荐：有效目标 = 基础目标 + 运动
            if (user.getDailyCalorieTarget() != null) {
                calorieTarget = Math.round((user.getDailyCalorieTarget() + exerciseCalories * EXERCISE_CREDIT_RATIO) * 10.0) / 10.0;
                proteinTarget = CalorieCalculator.macroGrams(calorieTarget, user.getProteinRatio(), 4);
                fatTarget = CalorieCalculator.macroGrams(calorieTarget, user.getFatRatio(), 9);
                carbsTarget = CalorieCalculator.macroGrams(calorieTarget, user.getCarbsRatio(), 4);
            }
            // 最大：维持热量 TDEE + 运动
            if (user.getTdee() != null) {
                calorieMax = Math.round((user.getTdee() + exerciseCalories * EXERCISE_CREDIT_RATIO) * 10.0) / 10.0;
                proteinMax = CalorieCalculator.macroGrams(calorieMax, user.getProteinRatio(), 4);
                fatMax = CalorieCalculator.macroGrams(calorieMax, user.getFatRatio(), 9);
                carbsMax = CalorieCalculator.macroGrams(calorieMax, user.getCarbsRatio(), 4);
            }
        }

        return new DailyNutritionResponse(
                Math.round(protein * 10.0) / 10.0,
                Math.round(fat * 10.0) / 10.0,
                Math.round(carbs * 10.0) / 10.0,
                Math.round(calories * 10.0) / 10.0,
                proteinTarget, fatTarget, carbsTarget,
                proteinMax, fatMax, carbsMax,
                calorieTarget, calorieMax
        );
    }

    // 查某天记录（按餐次分组）
    public DailyMealResponse getDailySummary(Long userId, LocalDate date) {
        List<MealRecordResponse> allRecords = getRecordsByDate(userId, date);

        // 按餐次分组
        Map<String, List<MealRecordResponse>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(MealRecordResponse::getMealType));

        // 确保四种餐次都有（即使是空列表）
        for (String type : List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK")) {
            grouped.putIfAbsent(type, List.of());
        }

        double total = allRecords.stream()
                .mapToDouble(MealRecordResponse::getTotalCalories)
                .sum();

        return DailyMealResponse.builder()
                .mealDate(date)
                .totalCalories(Math.round(total * 10.0) / 10.0)
                .meals(grouped)
                .build();
    }
}