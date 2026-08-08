package com.example.calorieserver.repository;

import com.example.calorieserver.entity.SleepRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepRecordRepository extends JpaRepository<SleepRecord, Long> {

    // 查某用户某天的睡眠记录（同天覆盖，最多一条）
    Optional<SleepRecord> findByUserIdAndDate(Long userId, LocalDate date);

    // 查某用户某时间段的睡眠记录
    List<SleepRecord> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 当天睡眠总时长
    @Query("SELECT COALESCE(SUM(s.durationMin), 0) FROM SleepRecord s WHERE s.user.id = :userId AND s.date = :date")
    Integer sumMinByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 按日分组统计（趋势图用）
    @Query("SELECT s.date, SUM(s.durationMin) FROM SleepRecord s WHERE s.user.id = :userId AND s.date BETWEEN :start AND :end GROUP BY s.date ORDER BY s.date")
    List<Object[]> sumMinGroupByDate(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
