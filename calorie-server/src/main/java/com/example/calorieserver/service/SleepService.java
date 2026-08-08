package com.example.calorieserver.service;

import com.example.calorieserver.entity.SleepRecord;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.SleepRecordRepository;
import com.example.calorieserver.security.SecurityUtil;
import com.example.calorieserver.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SleepService {

    private final SleepRecordRepository sleepRecordRepository;

    /**
     * 新增/覆盖当天睡眠记录（同一天只保留一条）
     */
    public SleepRecord saveSleep(Long userId, LocalDate date, int durationMin) {
        Optional<SleepRecord> existing = sleepRecordRepository.findByUserIdAndDate(userId, date);
        if (existing.isPresent()) {
            SleepRecord record = existing.get();
            record.setDurationMin(durationMin);
            return sleepRecordRepository.save(record);
        }
        SleepRecord record = SleepRecord.builder()
                .user(User.builder().id(userId).build())
                .date(date)
                .durationMin(durationMin)
                .build();
        return sleepRecordRepository.save(record);
    }

    /**
     * 查询某天睡眠记录
     */
    public SleepRecord getByDate(Long userId, LocalDate date) {
        return sleepRecordRepository.findByUserIdAndDate(userId, date).orElse(null);
    }

    /**
     * 某天睡眠汇总（date 为 null 时默认今天）
     */
    public Map<String, Object> getSummary(Long userId, LocalDate date) {
        if (date == null) {
            date = TimeUtil.today();
        }
        SleepRecord record = sleepRecordRepository.findByUserIdAndDate(userId, date).orElse(null);
        Integer totalMin = sleepRecordRepository.sumMinByUserIdAndDate(userId, date);

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("durationMin", record != null ? record.getDurationMin() : 0);
        result.put("totalMin", totalMin != null ? totalMin : 0);
        result.put("date", date.toString());
        return result;
    }

    /**
     * 近 N 天睡眠时长趋势（按日期汇总）
     */
    public Map<LocalDate, Integer> getTrend(Long userId, LocalDate endDate, int days) {
        LocalDate startDate = endDate.minusDays(days - 1);
        List<Object[]> results = sleepRecordRepository.sumMinGroupByDate(userId, startDate, endDate);

        Map<LocalDate, Integer> trend = new LinkedHashMap<>();
        for (Object[] row : results) {
            trend.put((LocalDate) row[0], ((Number) row[1]).intValue());
        }
        return trend;
    }

    /**
     * 更新睡眠记录
     */
    public SleepRecord updateSleep(Long recordId, int durationMin) {
        SleepRecord record = sleepRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("睡眠记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        record.setDurationMin(durationMin);
        return sleepRecordRepository.save(record);
    }

    /**
     * 删除睡眠记录
     */
    public void deleteSleep(Long recordId) {
        SleepRecord record = sleepRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("睡眠记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        sleepRecordRepository.delete(record);
    }
}
