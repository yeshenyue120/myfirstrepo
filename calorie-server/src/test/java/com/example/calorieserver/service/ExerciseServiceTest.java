package com.example.calorieserver.service;

import com.example.calorieserver.dto.ExerciseRequest;
import com.example.calorieserver.entity.ExerciseRecord;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.ExerciseRecordRepository;
import com.example.calorieserver.security.ForbiddenException;
import com.example.calorieserver.util.TimeUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ExerciseService 单元测试：MET 热量计算、增删改查、趋势、最近类型去重。
 */
@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRecordRepository exerciseRecordRepository;

    @InjectMocks
    private ExerciseService exerciseService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** 以指定 userId 登录（模拟 JWT 已写入 SecurityContext） */
    private static void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    // ===== MET 热量计算 =====

    @Test
    void calculateCalories_formula() {
        // MET × 体重 × 小时 × 1.05
        assertEquals(441.0, exerciseService.calculateCalories(6.0, 70.0, 60)); // 6×70×1×1.05
        assertEquals(94.5, exerciseService.calculateCalories(3.0, 60.0, 30));  // 3×60×0.5×1.05
        assertEquals(158.0, exerciseService.calculateCalories(4.3, 70.0, 30)); // 4.3×70×0.5×1.05≈158.03
    }

    // ===== 新增 =====

    @Test
    void addExercise_defaultDateToday_computesCalories() {
        ExerciseRequest req = new ExerciseRequest("慢跑", 60, 6.0, null);
        when(exerciseRecordRepository.save(any(ExerciseRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseRecord saved = exerciseService.addExercise(1L, req, 70.0);

        assertEquals(TimeUtil.today(), saved.getRecordDate());
        assertEquals(441.0, saved.getCaloriesBurned());
        assertEquals("慢跑", saved.getExerciseType());
        assertEquals(1L, saved.getUser().getId());
    }

    @Test
    void addExercise_usesProvidedDate() {
        ExerciseRequest req = new ExerciseRequest("快走", 30, 4.3, "2026-08-01");
        when(exerciseRecordRepository.save(any(ExerciseRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseRecord saved = exerciseService.addExercise(1L, req, 70.0);

        assertEquals(LocalDate.of(2026, 8, 1), saved.getRecordDate());
        assertEquals(158.0, saved.getCaloriesBurned());
    }

    // ===== 查询 =====

    @Test
    void getByDate_defaultToday() {
        when(exerciseRecordRepository.findByUserIdAndRecordDateOrderByIdDesc(1L, TimeUtil.today()))
                .thenReturn(List.of(ExerciseRecord.builder().id(1L).build()));

        List<ExerciseRecord> list = exerciseService.getByDate(1L, null);

        assertEquals(1, list.size());
    }

    @Test
    void getSummary_totalsAndCount() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        ExerciseRecord r1 = ExerciseRecord.builder().caloriesBurned(200.0).recordDate(date).build();
        when(exerciseRecordRepository.findByUserIdAndRecordDateOrderByIdDesc(1L, date)).thenReturn(List.of(r1));
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, date)).thenReturn(500.0);

        Map<String, Object> s = exerciseService.getSummary(1L, date);

        assertEquals(500.0, s.get("totalCalories"));
        assertEquals(1, s.get("count"));
        assertEquals("2026-08-01", s.get("date"));
    }

    @Test
    void getSummary_nullDate_usesToday_nullTotalBecomesZero() {
        when(exerciseRecordRepository.findByUserIdAndRecordDateOrderByIdDesc(1L, TimeUtil.today())).thenReturn(List.of());
        when(exerciseRecordRepository.sumCaloriesByUserIdAndDate(1L, TimeUtil.today())).thenReturn(null);

        Map<String, Object> s = exerciseService.getSummary(1L, null);

        // 三元表达式 Double? : int 统一提升为 double，null 时值是 0.0 而非 0
        assertEquals(0.0, s.get("totalCalories"));
        assertEquals(TimeUtil.today().toString(), s.get("date"));
    }

    // ===== 更新 =====

    @Test
    void updateExercise_recalculatesCalories() {
        ExerciseRecord rec = ExerciseRecord.builder().id(5L).user(User.builder().id(1L).build()).build();
        when(exerciseRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        when(exerciseRecordRepository.save(any(ExerciseRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAs(1L);

        ExerciseRequest req = new ExerciseRequest("跑步", 45, 10.0, null);
        ExerciseRecord updated = exerciseService.updateExercise(5L, req, 70.0);

        assertEquals("跑步", updated.getExerciseType());
        assertEquals(45, updated.getDurationMin());
        assertEquals(551.3, updated.getCaloriesBurned()); // 10×70×0.75×1.05=551.25
    }

    @Test
    void updateExercise_notFound_throws() {
        when(exerciseRecordRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> exerciseService.updateExercise(99L, new ExerciseRequest("a", 30, 3.0, null), 70.0));
    }

    @Test
    void updateExercise_wrongOwner_forbidden() {
        ExerciseRecord rec = ExerciseRecord.builder().id(5L).user(User.builder().id(1L).build()).build();
        when(exerciseRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        loginAs(2L); // 不是记录归属者

        assertThrows(ForbiddenException.class,
                () -> exerciseService.updateExercise(5L, new ExerciseRequest("a", 30, 3.0, null), 70.0));
    }

    // ===== 删除 =====

    @Test
    void deleteExercise_ownerDeletes() {
        ExerciseRecord rec = ExerciseRecord.builder().id(5L).user(User.builder().id(1L).build()).build();
        when(exerciseRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        loginAs(1L);

        exerciseService.deleteExercise(5L);

        verify(exerciseRecordRepository).delete(rec);
    }

    @Test
    void deleteExercise_notFound_throws() {
        when(exerciseRecordRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> exerciseService.deleteExercise(5L));
    }

    // ===== 趋势 & 最近类型 =====

    @Test
    void getExerciseTrend_mapsRows() {
        LocalDate end = LocalDate.of(2026, 8, 7);
        List<Object[]> rows = List.<Object[]>of(new Object[]{LocalDate.of(2026, 8, 1), 300.0});
        when(exerciseRecordRepository.sumCaloriesGroupByDate(1L, LocalDate.of(2026, 8, 1), end)).thenReturn(rows);

        Map<LocalDate, Double> trend = exerciseService.getExerciseTrend(1L, end, 7);

        assertEquals(300.0, trend.get(LocalDate.of(2026, 8, 1)));
    }

    @Test
    void getRecentExerciseTypes_dedupPreservesOrder_capsAt10() {
        List<ExerciseRecord> recent = List.of(
                rec("跑步"), rec("游泳"), rec("散步"), rec("瑜伽"), rec("跳绳"),
                rec("单车"), rec("篮球"), rec("羽毛球"), rec("拳击"), rec("太极"),
                rec("网球"), rec("跑步"), rec("足球"));
        when(exerciseRecordRepository.findTop50ByUserIdOrderByRecordDateDescIdDesc(1L)).thenReturn(recent);

        List<String> types = exerciseService.getRecentExerciseTypes(1L);

        assertEquals(List.of("跑步", "游泳", "散步", "瑜伽", "跳绳", "单车", "篮球", "羽毛球", "拳击", "太极"), types);
    }

    private static ExerciseRecord rec(String type) {
        return ExerciseRecord.builder().exerciseType(type).build();
    }
}
