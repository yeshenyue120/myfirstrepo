package com.example.calorieserver.repository;

import com.example.calorieserver.entity.WaterRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaterRecordRepository extends JpaRepository<WaterRecord, Long> {

    // 查某用户某天的所有喝水记录
    List<WaterRecord> findByUserIdAndDateOrderByIdDesc(Long userId, LocalDate date);

    // 查某用户某时间段的喝水记录
    List<WaterRecord> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 当天喝水量合计
    @Query("SELECT COALESCE(SUM(w.amountMl), 0) FROM WaterRecord w WHERE w.user.id = :userId AND w.date = :date")
    Integer sumMlByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 按日分组统计（趋势图用）
    @Query("SELECT w.date, SUM(w.amountMl) FROM WaterRecord w WHERE w.user.id = :userId AND w.date BETWEEN :start AND :end GROUP BY w.date ORDER BY w.date")
    List<Object[]> sumMlGroupByDate(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
