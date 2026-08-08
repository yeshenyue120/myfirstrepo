package com.example.calorieserver.repository;

import com.example.calorieserver.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    // 查用户所有体重记录（按日期+ID降序，最新插入的在前）
    List<WeightRecord> findByUserIdOrderByRecordedDateDescIdDesc(Long userId);

    // 最近 2 条（用于首页最新体重+差值）
    List<WeightRecord> findTop2ByUserIdOrderByRecordedDateDescIdDesc(Long userId);

    // 查某用户某时间段的体重记录（按日期+ID升序，趋势图 x 轴从左到右）
    List<WeightRecord> findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(
            Long userId, LocalDate startDate, LocalDate endDate
    );

    // 查某用户某天的体重记录（有多条时取最新 ID）
    List<WeightRecord> findByUserIdAndRecordedDateOrderByIdDesc(Long userId, LocalDate date);

    // 查最近 N 条记录（用于趋势图）
    List<WeightRecord> findTop30ByUserIdOrderByRecordedDateDesc(Long userId);

    // 用户所有有体重记录的日期（打卡统计用）
    @Query("SELECT DISTINCT w.recordedDate FROM WeightRecord w WHERE w.user.id = :userId")
    List<LocalDate> findDistinctDatesByUserId(@Param("userId") Long userId);
}