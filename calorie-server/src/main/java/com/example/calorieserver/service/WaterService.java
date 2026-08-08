package com.example.calorieserver.service;

import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WaterRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.WaterRecordRepository;
import com.example.calorieserver.security.SecurityUtil;
import com.example.calorieserver.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WaterService {

    private final WaterRecordRepository waterRecordRepository;

    /**
     * 新增喝水记录
     */
    public WaterRecord addWater(Long userId, LocalDate date, int amountMl) {
        WaterRecord record = WaterRecord.builder()
                .user(User.builder().id(userId).build())
                .date(date)
                .amountMl(amountMl)
                .build();
        return waterRecordRepository.save(record);
    }

    /**
     * 查询某天所有喝水记录
     */
    public List<WaterRecord> getByDate(Long userId, LocalDate date) {
        return waterRecordRepository.findByUserIdAndDateOrderByIdDesc(userId, date);
    }

    /**
     * 某天喝水汇总（date 为 null 时默认今天）
     */
    public Map<String, Object> getSummary(Long userId, LocalDate date) {
        if (date == null) {
            date = TimeUtil.today();
        }
        List<WaterRecord> records = waterRecordRepository.findByUserIdAndDateOrderByIdDesc(userId, date);
        Integer totalMl = waterRecordRepository.sumMlByUserIdAndDate(userId, date);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("totalMl", totalMl != null ? totalMl : 0);
        result.put("count", records.size());
        result.put("date", date.toString());
        return result;
    }

    /**
     * 近 N 天喝水量趋势（按日期汇总）
     */
    public Map<LocalDate, Integer> getTrend(Long userId, LocalDate endDate, int days) {
        LocalDate startDate = endDate.minusDays(days - 1);
        List<Object[]> results = waterRecordRepository.sumMlGroupByDate(userId, startDate, endDate);

        Map<LocalDate, Integer> trend = new LinkedHashMap<>();
        for (Object[] row : results) {
            trend.put((LocalDate) row[0], ((Number) row[1]).intValue());
        }
        return trend;
    }

    /**
     * 更新喝水记录
     */
    public WaterRecord updateWater(Long recordId, int amountMl) {
        WaterRecord record = waterRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("喝水记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        record.setAmountMl(amountMl);
        return waterRecordRepository.save(record);
    }

    /**
     * 删除喝水记录
     */
    public void deleteWater(Long recordId) {
        WaterRecord record = waterRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("喝水记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        waterRecordRepository.delete(record);
    }
}
