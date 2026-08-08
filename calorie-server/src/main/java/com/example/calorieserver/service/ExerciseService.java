package com.example.calorieserver.service;

import com.example.calorieserver.dto.ExerciseRequest;
import com.example.calorieserver.entity.ExerciseRecord;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.ExerciseRecordRepository;
import com.example.calorieserver.security.SecurityUtil;
import com.example.calorieserver.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRecordRepository exerciseRecordRepository;

    // 常量：每公斤体重每小时消耗的 kcal 估算值
    private static final double KCAL_PER_KG_PER_MET_HOUR = 1.05;

    /**
     * 计算运动消耗热量
     * 公式：MET × 体重(kg) × 时长(小时) × 1.05
     */
    public double calculateCalories(double metValue, double weightKg, int durationMin) {
        double hours = durationMin / 60.0;
        return Math.round(metValue * weightKg * hours * KCAL_PER_KG_PER_MET_HOUR * 10.0) / 10.0;
    }

    /**
     * 新增运动记录（前端已算好热量）
     */
    public ExerciseRecord addExercise(Long userId, ExerciseRequest request, double weightKg) {
        LocalDate date = (request.getRecordDate() != null && !request.getRecordDate().isEmpty())
                ? LocalDate.parse(request.getRecordDate())
                : TimeUtil.today();

        double calories = calculateCalories(request.getMetValue(), weightKg, request.getDurationMin());

        ExerciseRecord record = ExerciseRecord.builder()
                .user(User.builder().id(userId).build())
                .exerciseType(request.getExerciseType())
                .durationMin(request.getDurationMin())
                .metValue(request.getMetValue())
                .caloriesBurned(calories)
                .recordDate(date)
                .build();
        return exerciseRecordRepository.save(record);
    }

    /**
     * 查询某天所有运动记录
     */
    public List<ExerciseRecord> getByDate(Long userId, String dateStr) {
        LocalDate date = (dateStr != null && !dateStr.isEmpty())
                ? LocalDate.parse(dateStr)
                : TimeUtil.today();
        return exerciseRecordRepository.findByUserIdAndRecordDateOrderByIdDesc(userId, date);
    }

    /**
     * 某天运动汇总（date 为 null 时默认今天）
     */
    public Map<String, Object> getSummary(Long userId, LocalDate date) {
        if (date == null) {
            date = TimeUtil.today();
        }
        List<ExerciseRecord> records = exerciseRecordRepository.findByUserIdAndRecordDateOrderByIdDesc(userId, date);
        Double totalCalories = exerciseRecordRepository.sumCaloriesByUserIdAndDate(userId, date);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("totalCalories", totalCalories != null ? totalCalories : 0);
        result.put("count", records.size());
        result.put("date", date.toString());
        return result;
    }

    /**
     * 更新运动记录
     */
    public ExerciseRecord updateExercise(Long recordId, ExerciseRequest request, double weightKg) {
        ExerciseRecord record = exerciseRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("运动记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        record.setExerciseType(request.getExerciseType());
        record.setDurationMin(request.getDurationMin());
        record.setMetValue(request.getMetValue());
        double calories = calculateCalories(request.getMetValue(), weightKg, request.getDurationMin());
        record.setCaloriesBurned(calories);
        return exerciseRecordRepository.save(record);
    }

    /**
     * 删除运动记录
     */
    public void deleteExercise(Long recordId) {
        ExerciseRecord record = exerciseRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("运动记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        exerciseRecordRepository.delete(record);
    }

    /**
     * 近 N 天运动趋势
     */
    public Map<LocalDate, Double> getExerciseTrend(Long userId, LocalDate endDate, int days) {
        LocalDate startDate = endDate.minusDays(days - 1);
        List<Object[]> results = exerciseRecordRepository.sumCaloriesGroupByDate(userId, startDate, endDate);
        Map<LocalDate, Double> trend = new LinkedHashMap<>();
        for (Object[] row : results) {
            trend.put((LocalDate) row[0], (Double) row[1]);
        }
        return trend;
    }

    /**
     * 最近使用过的运动类型（按最近使用倒序去重，取前 10 个）
     */
    public List<String> getRecentExerciseTypes(Long userId) {
        List<ExerciseRecord> recent = exerciseRecordRepository.findTop50ByUserIdOrderByRecordDateDescIdDesc(userId);
        LinkedHashSet<String> types = new LinkedHashSet<>();
        for (ExerciseRecord rec : recent) {
            types.add(rec.getExerciseType());
            if (types.size() >= 10) break;
        }
        return new ArrayList<>(types);
    }
}
