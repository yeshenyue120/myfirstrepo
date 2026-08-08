package com.example.calorieserver.repository;

import com.example.calorieserver.entity.MealRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    // 查某用户某一天的所有饮食记录（food 一次 fetch，避免逐条懒加载 N+1）
    @EntityGraph(attributePaths = "food")
    List<MealRecord> findByUserIdAndMealDate(Long userId, LocalDate mealDate);

    // 查某用户某一天的某一餐
    @EntityGraph(attributePaths = "food")
    List<MealRecord> findByUserIdAndMealDateAndMealType(
            Long userId, LocalDate mealDate, MealRecord.MealType mealType
    );

    // 查某用户某时间段的饮食记录
    @EntityGraph(attributePaths = "food")
    List<MealRecord> findByUserIdAndMealDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate
    );

    // 删除某天的所有记录
    void deleteByUserIdAndMealDate(Long userId, LocalDate mealDate);

    // 统计某用户某天的记录数
    long countByUserIdAndMealDate(Long userId, LocalDate mealDate);

    // 某食物是否被任何饮食记录引用（删除自定义食物前检查）
    boolean existsByFoodId(Long foodId);

    // ===== 新增：统计一天总热量 =====
    // JPQL：SUM 聚合函数，查不到时返回 null（不是 0）
    @Query("SELECT SUM(m.totalCalories) FROM MealRecord m WHERE m.user.id = :userId AND m.mealDate = :date")
    Optional<Double> sumCaloriesByUserIdAndMealDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    // ===== 新增：按日分组统计（趋势图用）=====
    // 返回 Object[]：第一个元素是日期，第二个元素是当天总热量
    @Query("SELECT m.mealDate, SUM(m.totalCalories) FROM MealRecord m WHERE m.user.id = :userId AND m.mealDate BETWEEN :start AND :end GROUP BY m.mealDate ORDER BY m.mealDate")
    List<Object[]> sumCaloriesGroupByDate(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // 用户所有有饮食记录的日期（打卡统计用）
    @Query("SELECT DISTINCT m.mealDate FROM MealRecord m WHERE m.user.id = :userId")
    List<LocalDate> findDistinctDatesByUserId(@Param("userId") Long userId);
}