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
import com.example.calorieserver.security.ForbiddenException;
import com.example.calorieserver.util.CalorieCalculator;
import com.example.calorieserver.util.TimeUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MealRecordService 单元测试：批量/单条添加（热量自动计算）、查询、修改/删除（归属校验）、
 * 当日总热量、趋势、每日营养汇总（运动×0.9 计入目标）、三餐分组汇总。
 */
@ExtendWith(MockitoExtension.class)
class MealRecordServiceTest {

    @Mock
    private MealRecordRepository mealRecordRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExerciseRecordRepository exerciseRecordRepository;

    @InjectMocks
    private MealRecordService mealRecordService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private Food food(Long id, String name, double cal, Double protein, Double fat, Double carbs) {
        return Food.builder().id(id).name(name).caloriesPer100g(cal)
                .proteinPer100g(protein).fatPer100g(fat).carbsPer100g(carbs)
                .isPublic(true).isCommon(true).build();
    }

    private MealRecord rec(Long id, Long userId, Food food, double grams, double totalCalories,
                           MealRecord.MealType type, LocalDate date) {
        return MealRecord.builder().id(id).user(User.builder().id(userId).build())
                .food(food).grams(grams).totalCalories(totalCalories)
                .mealType(type).mealDate(date).build();
    }

    private MealRecordRequest req(Long foodId, double grams, MealRecord.MealType type, LocalDate date) {
        MealRecordRequest r = new MealRecordRequest();
        r.setFoodId(foodId);
        r.setGrams(grams);
        r.setMealType(type);
        r.setMealDate(date);
        return r;
    }

    // ===== 批量添加 =====

    @Test
    void batchAddRecords_success_computesCalories() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Food rice = food(1L, "米饭", 100.0, 2.6, 0.3, 25.9);
        Food egg = food(2L, "鸡蛋", 200.0, 13.3, 8.8, 2.8);
        when(foodRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(rice, egg));
        when(mealRecordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<MealRecordResponse> resp = mealRecordService.batchAddRecords(1L, List.of(
                req(1L, 150.0, MealRecord.MealType.BREAKFAST, date),
                req(2L, 50.0, MealRecord.MealType.LUNCH, date)));

        assertEquals(2, resp.size());
        assertEquals(150.0, resp.get(0).getTotalCalories(), 0.001); // 150/100×100
        assertEquals("米饭", resp.get(0).getFoodName());
        assertEquals("BREAKFAST", resp.get(0).getMealType());
        assertEquals(100.0, resp.get(1).getTotalCalories(), 0.001); // 50/100×200
        verify(mealRecordRepository).saveAll(anyList());
    }

    @Test
    void batchAddRecords_foodMissing_throws() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(foodRepository.findAllById(List.of(999L))).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> mealRecordService.batchAddRecords(1L, List.of(req(999L, 100.0, MealRecord.MealType.SNACK, date))));
        assertEquals("食物不存在: 999", ex.getMessage());
    }

    // ===== 单条添加 =====

    @Test
    void addRecord_foodNotFound_throws() {
        when(foodRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> mealRecordService.addRecord(1L, req(1L, 100.0, MealRecord.MealType.DINNER, LocalDate.of(2026, 8, 1))));
    }

    @Test
    void addRecord_success_computesCalories() {
        Food rice = food(1L, "米饭", 100.0, 2.6, 0.3, 25.9);
        when(foodRepository.findById(1L)).thenReturn(Optional.of(rice));
        when(mealRecordRepository.save(any(MealRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MealRecordResponse resp = mealRecordService.addRecord(1L, req(1L, 150.0, MealRecord.MealType.DINNER, LocalDate.of(2026, 8, 1)));

        assertEquals(150.0, resp.getTotalCalories(), 0.001);
        assertEquals(1L, resp.getFoodId());
        assertEquals("米饭", resp.getFoodName());
        assertEquals("DINNER", resp.getMealType());
    }

    // ===== 查询 =====

    @Test
    void getRecordsByDate_mapsResponses() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date))
                .thenReturn(List.of(rec(1L, 1L, food(1L, "米饭", 100.0, null, null, null), 100.0, 100.0, MealRecord.MealType.LUNCH, date)));

        List<MealRecordResponse> list = mealRecordService.getRecordsByDate(1L, date);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getFoodId());
        assertEquals(100.0, list.get(0).getTotalCalories(), 0.001);
        assertEquals(date, list.get(0).getMealDate());
    }

    @Test
    void getRecordsByDateAndMeal_passthrough() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(mealRecordRepository.findByUserIdAndMealDateAndMealType(1L, date, MealRecord.MealType.BREAKFAST))
                .thenReturn(List.of(rec(1L, 1L, food(1L, "鸡蛋", 144.0, null, null, null), 50.0, 72.0, MealRecord.MealType.BREAKFAST, date)));

        List<MealRecordResponse> list = mealRecordService.getRecordsByDateAndMeal(1L, date, MealRecord.MealType.BREAKFAST);

        assertEquals(1, list.size());
        assertEquals("鸡蛋", list.get(0).getFoodName());
    }

    @Test
    void getYesterdayMeal_usesYesterdayAndUppercasesType() {
        LocalDate yesterday = TimeUtil.today().minusDays(1);
        when(mealRecordRepository.findByUserIdAndMealDateAndMealType(eq(1L), eq(yesterday), eq(MealRecord.MealType.LUNCH)))
                .thenReturn(List.of(rec(1L, 1L, food(1L, "米饭", 100.0, null, null, null), 100.0, 100.0, MealRecord.MealType.LUNCH, yesterday)));

        List<MealRecordResponse> list = mealRecordService.getYesterdayMeal(1L, "lunch");

        assertEquals(1, list.size());
    }

    // ===== 修改 =====

    @Test
    void updateRecord_notFound_throws() {
        when(mealRecordRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> mealRecordService.updateRecord(5L, 100.0));
    }

    @Test
    void updateRecord_wrongOwner_forbidden() {
        MealRecord record = rec(5L, 1L, food(1L, "米饭", 200.0, null, null, null), 100.0, 200.0, MealRecord.MealType.LUNCH, LocalDate.of(2026, 8, 1));
        when(mealRecordRepository.findById(5L)).thenReturn(Optional.of(record));
        loginAs(2L);

        assertThrows(ForbiddenException.class, () -> mealRecordService.updateRecord(5L, 100.0));
    }

    @Test
    void updateRecord_owner_recalculatesCalories() {
        Food rice = food(1L, "米饭", 200.0, null, null, null);
        MealRecord record = rec(5L, 1L, rice, 100.0, 200.0, MealRecord.MealType.LUNCH, LocalDate.of(2026, 8, 1));
        when(mealRecordRepository.findById(5L)).thenReturn(Optional.of(record));
        when(mealRecordRepository.save(any(MealRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAs(1L);

        MealRecordResponse resp = mealRecordService.updateRecord(5L, 50.0);

        assertEquals(50.0, resp.getGrams(), 0.001);
        assertEquals(100.0, resp.getTotalCalories(), 0.001); // 50/100×200
    }

    // ===== 删除 =====

    @Test
    void deleteRecord_notFound_throws() {
        when(mealRecordRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> mealRecordService.deleteRecord(5L));
    }

    @Test
    void deleteRecord_wrongOwner_forbidden() {
        MealRecord record = rec(5L, 1L, food(1L, "米饭", 100.0, null, null, null), 100.0, 100.0, MealRecord.MealType.LUNCH, LocalDate.of(2026, 8, 1));
        when(mealRecordRepository.findById(5L)).thenReturn(Optional.of(record));
        loginAs(2L);

        assertThrows(ForbiddenException.class, () -> mealRecordService.deleteRecord(5L));
    }

    @Test
    void deleteRecord_owner_deletes() {
        MealRecord record = rec(5L, 1L, food(1L, "米饭", 100.0, null, null, null), 100.0, 100.0, MealRecord.MealType.LUNCH, LocalDate.of(2026, 8, 1));
        when(mealRecordRepository.findById(5L)).thenReturn(Optional.of(record));
        loginAs(1L);

        mealRecordService.deleteRecord(5L);

        verify(mealRecordRepository).delete(record);
    }

    @Test
    void deleteRecordsByDate_callsRepo() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        mealRecordService.deleteRecordsByDate(1L, date);
        verify(mealRecordRepository).deleteByUserIdAndMealDate(1L, date);
    }

    // ===== 当日总热量 =====

    @Test
    void getTotalCalories_empty_returnsZero() {
        when(mealRecordRepository.sumCaloriesByUserIdAndMealDate(1L, LocalDate.of(2026, 8, 1))).thenReturn(Optional.empty());
        assertEquals(0.0, mealRecordService.getTotalCalories(1L, LocalDate.of(2026, 8, 1)), 0.001);
    }

    @Test
    void getTotalCalories_returnsSum() {
        when(mealRecordRepository.sumCaloriesByUserIdAndMealDate(1L, LocalDate.of(2026, 8, 1)))
                .thenReturn(Optional.of(520.5));
        assertEquals(520.5, mealRecordService.getTotalCalories(1L, LocalDate.of(2026, 8, 1)), 0.001);
    }

    // ===== 近 N 天趋势 =====

    @Test
    void getWeeklyTrend_mapsResults() {
        LocalDate end = LocalDate.of(2026, 8, 7);
        LocalDate d1 = LocalDate.of(2026, 8, 5);
        LocalDate d2 = LocalDate.of(2026, 8, 6);
        List<Object[]> rows = List.<Object[]>of(new Object[]{d1, 800.0}, new Object[]{d2, 600.0});
        when(mealRecordRepository.sumCaloriesGroupByDate(1L, LocalDate.of(2026, 8, 3), end)).thenReturn(rows);

        Map<LocalDate, Double> trend = mealRecordService.getWeeklyTrend(1L, end, 5);

        assertEquals(2, trend.size());
        assertEquals(800.0, trend.get(d1), 0.001);
        assertEquals(600.0, trend.get(d2), 0.001);
    }

    // ===== 每日营养汇总 =====

    @Test
    void getDailyNutrition_noUser_targetsNull() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, date)).thenReturn(null);

        DailyNutritionResponse resp = mealRecordService.getDailyNutrition(1L, date);

        assertEquals(0.0, resp.protein(), 0.001);
        assertEquals(0.0, resp.totalCalories(), 0.001);
        assertNull(resp.calorieTarget());
        assertNull(resp.calorieMax());
        assertNull(resp.proteinTarget());
        assertNull(resp.proteinMax());
    }

    @Test
    void getDailyNutrition_userWithTargets_noExercise() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(List.of());
        User user = User.builder().id(1L)
                .dailyCalorieTarget(2000.0).tdee(2500.0)
                .proteinRatio(30.0).fatRatio(25.0).carbsRatio(45.0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, date)).thenReturn(0.0);

        DailyNutritionResponse resp = mealRecordService.getDailyNutrition(1L, date);

        assertEquals(2000.0, resp.calorieTarget(), 0.001); // 目标 + 0 运动
        assertEquals(2500.0, resp.calorieMax(), 0.001);    // TDEE + 0 运动
        assertEquals(CalorieCalculator.macroGrams(2000.0, 30.0, 4), resp.proteinTarget());
        assertEquals(CalorieCalculator.macroGrams(2500.0, 30.0, 4), resp.proteinMax());
        assertEquals(CalorieCalculator.macroGrams(2000.0, 45.0, 4), resp.carbsTarget());
    }

    @Test
    void getDailyNutrition_exerciseIncluded() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(List.of());
        User user = User.builder().id(1L)
                .dailyCalorieTarget(2000.0).tdee(2500.0)
                .proteinRatio(30.0).fatRatio(25.0).carbsRatio(45.0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, date)).thenReturn(100.0);

        DailyNutritionResponse resp = mealRecordService.getDailyNutrition(1L, date);

        // 有效额度 = 基础目标 + 运动×0.9
        assertEquals(2090.0, resp.calorieTarget(), 0.001);
        assertEquals(2590.0, resp.calorieMax(), 0.001);
        assertEquals(CalorieCalculator.macroGrams(2090.0, 30.0, 4), resp.proteinTarget());
    }

    @Test
    void getDailyNutrition_accumulatesNutrition() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Food rice = food(1L, "米饭", 300.0, 20.0, 10.0, 50.0);
        MealRecord record = rec(1L, 1L, rice, 100.0, 300.0, MealRecord.MealType.LUNCH, date);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(List.of(record));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, date)).thenReturn(0.0);

        DailyNutritionResponse resp = mealRecordService.getDailyNutrition(1L, date);

        assertEquals(20.0, resp.protein(), 0.001);
        assertEquals(10.0, resp.fat(), 0.001);
        assertEquals(50.0, resp.carbs(), 0.001);
        assertEquals(300.0, resp.totalCalories(), 0.001);
    }

    @Test
    void getDailyNutrition_nullNutrition_treatsAsZero() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Food foodNoMacro = food(1L, "无营养数据", 200.0, null, null, null);
        MealRecord record = rec(1L, 1L, foodNoMacro, 100.0, 200.0, MealRecord.MealType.SNACK, date);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(List.of(record));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, date)).thenReturn(0.0);

        DailyNutritionResponse resp = mealRecordService.getDailyNutrition(1L, date);

        assertEquals(0.0, resp.protein(), 0.001);
        assertEquals(0.0, resp.fat(), 0.001);
        assertEquals(0.0, resp.carbs(), 0.001);
    }

    // ===== 三餐汇总 =====

    @Test
    void getDailySummary_groupsByMealType_ensuresAllFour() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Food rice = food(1L, "米饭", 100.0, null, null, null);
        List<MealRecord> records = List.of(
                rec(1L, 1L, rice, 100.0, 100.0, MealRecord.MealType.BREAKFAST, date),
                rec(2L, 1L, rice, 200.0, 200.0, MealRecord.MealType.BREAKFAST, date),
                rec(3L, 1L, rice, 300.0, 300.0, MealRecord.MealType.DINNER, date));
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(records);

        DailyMealResponse resp = mealRecordService.getDailySummary(1L, date);

        assertEquals(date, resp.getMealDate());
        assertEquals(600.0, resp.getTotalCalories(), 0.001);
        assertEquals(2, resp.getMeals().get("BREAKFAST").size());
        assertEquals(1, resp.getMeals().get("DINNER").size());
        // 未记录的两餐也必须有 key
        assertEquals(0, resp.getMeals().get("LUNCH").size());
        assertEquals(0, resp.getMeals().get("SNACK").size());
    }

    @Test
    void getDailySummary_empty_allMealKeysPresent() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(mealRecordRepository.findByUserIdAndMealDate(1L, date)).thenReturn(List.of());

        DailyMealResponse resp = mealRecordService.getDailySummary(1L, date);

        assertEquals(0.0, resp.getTotalCalories(), 0.001);
        assertEquals(4, resp.getMeals().size());
        assertTrue(resp.getMeals().containsKey("BREAKFAST"));
        assertTrue(resp.getMeals().containsKey("LUNCH"));
        assertTrue(resp.getMeals().containsKey("DINNER"));
        assertTrue(resp.getMeals().containsKey("SNACK"));
    }
}
