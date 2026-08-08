package com.example.calorieserver.service;

import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.repository.ExerciseRecordRepository;
import com.example.calorieserver.repository.MealRecordRepository;
import com.example.calorieserver.repository.UserRepository;
import com.example.calorieserver.repository.WeightRecordRepository;
import com.example.calorieserver.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * StatsService 单元测试：打卡连续（今天已打卡锚点/未打卡退昨天/中断/饮食∪体重合并）、
 * 周报/月报（摄入/运动/体重差值/打卡并集/目标达成天数/空数据兜底）。
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private MealRecordRepository mealRecordRepository;

    @Mock
    private WeightRecordRepository weightRecordRepository;

    @Mock
    private ExerciseRecordRepository exerciseRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatsService statsService;

    private WeightRecord w(LocalDate date, double kg) {
        return WeightRecord.builder().recordedDate(date).weightKg(kg).build();
    }

    private User userWithTarget(Double target) {
        return User.builder().id(1L).dailyCalorieTarget(target).build();
    }

    // ===== getStreak =====

    @Test
    void getStreak_empty_allZero() {
        when(mealRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of());
        when(weightRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of());

        Map<String, Object> m = statsService.getStreak(1L);

        assertEquals(0, m.get("currentStreak"));
        assertEquals(0, m.get("longestStreak"));
        assertNull(m.get("lastCheckInDate"));
        assertEquals(TimeUtil.today().toString(), m.get("today"));
    }

    @Test
    void getStreak_todayCheckedIn_anchorToday() {
        LocalDate today = TimeUtil.today();
        when(mealRecordRepository.findDistinctDatesByUserId(1L))
                .thenReturn(List.of(today.minusDays(2), today.minusDays(1), today));
        when(weightRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of());

        Map<String, Object> m = statsService.getStreak(1L);

        assertEquals(3, m.get("currentStreak"));
        assertEquals(3, m.get("longestStreak"));
        assertEquals(today, m.get("lastCheckInDate"));
    }

    @Test
    void getStreak_todayMissing_anchorFallsBackToYesterday() {
        LocalDate today = TimeUtil.today();
        when(mealRecordRepository.findDistinctDatesByUserId(1L))
                .thenReturn(List.of(today.minusDays(2), today.minusDays(1)));
        when(weightRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of());

        Map<String, Object> m = statsService.getStreak(1L);

        assertEquals(2, m.get("currentStreak")); // 从昨天往回连续 2 天
        assertEquals(2, m.get("longestStreak"));
        assertEquals(today.minusDays(1), m.get("lastCheckInDate"));
    }

    @Test
    void getStreak_breakInConsecutive_currentStopsAtGap() {
        LocalDate today = TimeUtil.today();
        // 今天有、昨天没有 → current 只算 1；最长连续是（今天-3, 今天-2）
        when(mealRecordRepository.findDistinctDatesByUserId(1L))
                .thenReturn(List.of(today.minusDays(3), today.minusDays(2), today));
        when(weightRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of());

        Map<String, Object> m = statsService.getStreak(1L);

        assertEquals(1, m.get("currentStreak"));
        assertEquals(2, m.get("longestStreak"));
    }

    @Test
    void getStreak_mergesMealAndWeight() {
        LocalDate today = TimeUtil.today();
        when(mealRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of(today));
        when(weightRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of(today.minusDays(1)));

        Map<String, Object> m = statsService.getStreak(1L);

        assertEquals(2, m.get("currentStreak")); // 饮食∪体重
        assertEquals(2, m.get("longestStreak"));
    }

    @Test
    void getStreak_singleRecord() {
        LocalDate today = TimeUtil.today();
        when(mealRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of(today.minusDays(1)));
        when(weightRecordRepository.findDistinctDatesByUserId(1L)).thenReturn(List.of());

        Map<String, Object> m = statsService.getStreak(1L);

        assertEquals(1, m.get("currentStreak"));
        assertEquals(1, m.get("longestStreak"));
    }

    // ===== getReport =====

    private void stubEmptyReportQuery() {
        when(mealRecordRepository.sumCaloriesGroupByDate(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
                .thenReturn(List.of());
        when(exerciseRecordRepository.sumCaloriesGroupByDate(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
                .thenReturn(List.of());
        when(weightRecordRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(
                1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
                .thenReturn(new ArrayList<>());
    }

    @Test
    void getReport_emptyData_allDefaults() {
        stubEmptyReportQuery();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> m = statsService.getReport(1L, LocalDate.of(2026, 8, 3), 3);

        assertEquals(0.0, m.get("totalCalories"));
        assertEquals(0.0, m.get("averageCalories"));
        assertEquals(0.0, m.get("exerciseCalories"));
        assertNull(m.get("startWeight"));
        assertNull(m.get("endWeight"));
        assertNull(m.get("weightChange"));
        assertNull(m.get("dailyCalorieTarget"));
        assertEquals(0, m.get("goalDays"));
        assertEquals(List.of(), m.get("checkInDates"));
        assertTrue(((Map<?, ?>) m.get("intakeTrend")).isEmpty());
    }

    @Test
    void getReport_full_computesTotalsAndGoalDays() {
        LocalDate d1 = LocalDate.of(2026, 8, 1);
        LocalDate d2 = LocalDate.of(2026, 8, 2);
        LocalDate d3 = LocalDate.of(2026, 8, 3);
        when(mealRecordRepository.sumCaloriesGroupByDate(1L, d1, d3))
                .thenReturn(List.<Object[]>of(new Object[]{d1, 1900.0}, new Object[]{d2, 2100.0}));
        when(exerciseRecordRepository.sumCaloriesGroupByDate(1L, d1, d3))
                .thenReturn(List.<Object[]>of(new Object[]{d1, 300.0}, new Object[]{d2, 100.0}));
        when(weightRecordRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(1L, d1, d3))
                .thenReturn(new ArrayList<>(List.of(w(d3, 69.0), w(d1, 70.0)))); // 乱序传入，service 内部会排序
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithTarget(2000.0)));

        Map<String, Object> m = statsService.getReport(1L, d3, 3);

        assertEquals(4000.0, m.get("totalCalories"));
        assertEquals(2000.0, m.get("averageCalories"));
        assertEquals(400.0, m.get("exerciseCalories"));
        assertEquals(70.0, m.get("startWeight"));
        assertEquals(69.0, m.get("endWeight"));
        assertEquals(-1.0, m.get("weightChange"));
        assertEquals(2000.0, m.get("dailyCalorieTarget"));
        // d1=1900、d2=2100 都在 target ±10%（1800~2200）内 → 2 天达标
        assertEquals(2, m.get("goalDays"));
        // 打卡 = 摄入日期 {d1,d2} ∪ 体重日期 {d1,d3}
        assertEquals(List.of("2026-08-01", "2026-08-02", "2026-08-03"), m.get("checkInDates"));
        assertEquals(2, ((Map<?, ?>) m.get("intakeTrend")).size());
    }

    @Test
    void getReport_singleWeight_noChange() {
        LocalDate d1 = LocalDate.of(2026, 8, 1);
        LocalDate d3 = LocalDate.of(2026, 8, 3);
        when(mealRecordRepository.sumCaloriesGroupByDate(1L, d1, d3)).thenReturn(List.of());
        when(exerciseRecordRepository.sumCaloriesGroupByDate(1L, d1, d3)).thenReturn(List.of());
        when(weightRecordRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(1L, d1, d3))
                .thenReturn(new ArrayList<>(List.of(w(d3, 69.0))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithTarget(2000.0)));

        Map<String, Object> m = statsService.getReport(1L, d3, 3);

        assertNull(m.get("startWeight"));
        assertEquals(69.0, m.get("endWeight"));
        assertNull(m.get("weightChange"));
    }

    @Test
    void getReport_userNull_goalDaysZero() {
        LocalDate d1 = LocalDate.of(2026, 8, 1);
        LocalDate d3 = LocalDate.of(2026, 8, 3);
        when(mealRecordRepository.sumCaloriesGroupByDate(1L, d1, d3))
                .thenReturn(List.<Object[]>of(new Object[]{d1, 1900.0}));
        when(exerciseRecordRepository.sumCaloriesGroupByDate(1L, d1, d3)).thenReturn(List.of());
        when(weightRecordRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(1L, d1, d3))
                .thenReturn(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> m = statsService.getReport(1L, d3, 3);

        assertNull(m.get("dailyCalorieTarget"));
        assertEquals(0, m.get("goalDays"));
    }

    @Test
    void getReport_intakeOutsideRange_notCounted() {
        LocalDate d1 = LocalDate.of(2026, 8, 1);
        LocalDate d2 = LocalDate.of(2026, 8, 2);
        LocalDate d3 = LocalDate.of(2026, 8, 3);
        // 1500 < 1800、2200.1 > 2200 → 都不达标
        when(mealRecordRepository.sumCaloriesGroupByDate(1L, d1, d3))
                .thenReturn(List.<Object[]>of(new Object[]{d1, 1500.0}, new Object[]{d2, 2200.1}));
        when(exerciseRecordRepository.sumCaloriesGroupByDate(1L, d1, d3)).thenReturn(List.of());
        when(weightRecordRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(1L, d1, d3))
                .thenReturn(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithTarget(2000.0)));

        Map<String, Object> m = statsService.getReport(1L, d3, 3);

        assertEquals(0, m.get("goalDays"));
    }
}
