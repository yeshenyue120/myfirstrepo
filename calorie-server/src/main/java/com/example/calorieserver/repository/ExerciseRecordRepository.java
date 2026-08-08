package com.example.calorieserver.repository;

import com.example.calorieserver.entity.ExerciseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {

    // 查某用户某天的所有运动记录
    List<ExerciseRecord> findByUserIdAndRecordDateOrderByIdDesc(Long userId, LocalDate date);

    // 查某用户某时间段的运动记录
    List<ExerciseRecord> findByUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 当天运动总消耗
    @Query("SELECT COALESCE(SUM(e.caloriesBurned), 0) FROM ExerciseRecord e WHERE e.user.id = :userId AND e.recordDate = :date")
    Double sumCaloriesByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 按日期分组汇总（趋势用）
    @Query("SELECT e.recordDate, SUM(e.caloriesBurned) FROM ExerciseRecord e WHERE e.user.id = :userId AND e.recordDate BETWEEN :start AND :end GROUP BY e.recordDate ORDER BY e.recordDate")
    List<Object[]> sumCaloriesGroupByDate(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // 查某用户最近的运动记录（按日期倒序，取前 50 条，用于提取最近使用的运动类型）
    List<ExerciseRecord> findTop50ByUserIdOrderByRecordDateDescIdDesc(Long userId);
}
