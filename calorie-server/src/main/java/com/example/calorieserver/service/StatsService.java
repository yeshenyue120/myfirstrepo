package com.example.calorieserver.service;

import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.repository.ExerciseRecordRepository;
import com.example.calorieserver.repository.MealRecordRepository;
import com.example.calorieserver.repository.UserRepository;
import com.example.calorieserver.repository.WeightRecordRepository;
import com.example.calorieserver.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final MealRecordRepository mealRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;

    /**
     * 打卡统计：打卡 = 当天有饮食记录或体重记录
     * 返回 { currentStreak, longestStreak, lastCheckInDate, today }
     */
    public Map<String, Object> getStreak(Long userId) {
        Set<LocalDate> dates = new HashSet<>();
        dates.addAll(mealRecordRepository.findDistinctDatesByUserId(userId));
        dates.addAll(weightRecordRepository.findDistinctDatesByUserId(userId));

        List<LocalDate> sorted = dates.stream().sorted().collect(Collectors.toList());

        // 当天打卡日期（今天未打卡则从昨天开始算连续）
        LocalDate today = TimeUtil.today();
        LocalDate anchor = today;
        if (sorted.isEmpty() || sorted.get(sorted.size() - 1).isBefore(anchor)) {
            anchor = today.minusDays(1);
        }

        // 当前连续天数
        int currentStreak = 0;
        if (!sorted.isEmpty()) {
            LocalDate expected = anchor;
            Set<LocalDate> dateSet = dates;
            while (dateSet.contains(expected)) {
                currentStreak++;
                expected = expected.minusDays(1);
            }
        }

        // 最长连续天数
        int longestStreak = 0;
        int run = 0;
        LocalDate prev = null;
        for (LocalDate d : sorted) {
            if (prev != null && d.equals(prev.plusDays(1))) {
                run++;
            } else {
                run = 1;
            }
            longestStreak = Math.max(longestStreak, run);
            prev = d;
        }

        LocalDate lastCheckInDate = sorted.isEmpty() ? null : sorted.get(sorted.size() - 1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentStreak", currentStreak);
        result.put("longestStreak", longestStreak);
        result.put("lastCheckInDate", lastCheckInDate);
        result.put("today", today.toString());
        return result;
    }

    /**
     * 周报/月报：返回指定时间段内的摄入/体重/运动/打卡/达成汇总。
     * 打卡 = 当天有饮食记录或体重记录。
     */
    public Map<String, Object> getReport(Long userId, LocalDate endDate, int days) {
        LocalDate startDate = endDate.minusDays(days - 1);

        // 摄入趋势（按日分组求和）
        List<Object[]> intakeRows = mealRecordRepository.sumCaloriesGroupByDate(userId, startDate, endDate);
        Map<LocalDate, Double> intakeTrend = new LinkedHashMap<>();
        double totalCalories = 0;
        for (Object[] row : intakeRows) {
            LocalDate d = (LocalDate) row[0];
            Double cals = (Double) row[1];
            intakeTrend.put(d, cals);
            totalCalories += cals;
        }
        int intakeDays = intakeTrend.size();

        // 运动总消耗
        List<Object[]> exRows = exerciseRecordRepository.sumCaloriesGroupByDate(userId, startDate, endDate);
        double exerciseCalories = 0;
        for (Object[] row : exRows) {
            exerciseCalories += (Double) row[1];
        }

        // 体重变化（首末差值）
        List<WeightRecord> weights = weightRecordRepository
                .findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(userId, startDate, endDate);
        weights.sort(Comparator.comparing(WeightRecord::getRecordedDate));
        Double startWeight = null, endWeight = null, weightChange = null;
        if (weights.size() >= 2) {
            startWeight = weights.get(0).getWeightKg();
            endWeight = weights.get(weights.size() - 1).getWeightKg();
            weightChange = Math.round((endWeight - startWeight) * 10.0) / 10.0;
        } else if (weights.size() == 1) {
            endWeight = weights.get(0).getWeightKg();
        }

        // 打卡日期：有摄入 ∪ 有体重
        Set<LocalDate> checkInSet = new TreeSet<>(intakeTrend.keySet());
        for (WeightRecord w : weights) {
            checkInSet.add(w.getRecordedDate());
        }
        List<String> checkInDates = checkInSet.stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());

        // 目标与达成天数（摄入在目标 ±10% 内）
        User user = userRepository.findById(userId).orElse(null);
        Double dailyCalorieTarget = user != null ? user.getDailyCalorieTarget() : null;
        int goalDays = 0;
        if (dailyCalorieTarget != null) {
            double lower = dailyCalorieTarget * 0.9;
            double upper = dailyCalorieTarget * 1.1;
            for (Double cals : intakeTrend.values()) {
                if (cals != null && cals >= lower && cals <= upper) {
                    goalDays++;
                }
            }
        }

        double averageCalories = intakeDays > 0
                ? Math.round(totalCalories / intakeDays * 10.0) / 10.0
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("days", days);
        result.put("dailyCalorieTarget", dailyCalorieTarget);
        result.put("totalCalories", Math.round(totalCalories * 10.0) / 10.0);
        result.put("averageCalories", averageCalories);
        result.put("exerciseCalories", Math.round(exerciseCalories * 10.0) / 10.0);
        result.put("startWeight", startWeight);
        result.put("endWeight", endWeight);
        result.put("weightChange", weightChange);
        result.put("checkInDates", checkInDates);
        result.put("goalDays", goalDays);
        result.put("intakeTrend", intakeTrend);
        return result;
    }
}
