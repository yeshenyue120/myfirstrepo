package com.example.calorieserver.service;

import com.example.calorieserver.dto.WeightHistoryPoint;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.WeightRecordRepository;
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
 * WeightService 单元测试：同日覆盖 + 清理重复、最新体重/差值、历史趋势、增删改（含归属校验与重算联动）。
 */
@ExtendWith(MockitoExtension.class)
class WeightServiceTest {

    @Mock
    private WeightRecordRepository weightRecordRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WeightService weightService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private WeightRecord rec(long id, double weightKg, LocalDate date) {
        return WeightRecord.builder().id(id).user(User.builder().id(1L).build()).weightKg(weightKg).recordedDate(date).build();
    }

    // ===== 记录体重（同日覆盖）=====

    @Test
    void addWeight_sameDay_updatesLatest_deletesDuplicates() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        WeightRecord latest = rec(2L, 70.0, date);
        WeightRecord older = rec(1L, 71.0, date);
        when(weightRecordRepository.findByUserIdAndRecordedDateOrderByIdDesc(1L, date)).thenReturn(List.of(latest, older));
        when(weightRecordRepository.save(any(WeightRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        // 重算联动：最新一条体重 → userService
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(latest));

        WeightRecord saved = weightService.addWeight(1L, 65.0, date, null, null, null);

        assertEquals(65.0, saved.getWeightKg());
        assertEquals(2L, saved.getId()); // 更新的是最新那条，而非新增
        verify(weightRecordRepository).deleteAll(List.of(older));
        verify(userService).recalculateTargetsFromWeight(1L, 65.0);
    }

    @Test
    void addWeight_sameDay_singleRecord_noDuplicateDeletion() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        WeightRecord latest = rec(2L, 70.0, date);
        when(weightRecordRepository.findByUserIdAndRecordedDateOrderByIdDesc(1L, date)).thenReturn(List.of(latest));
        when(weightRecordRepository.save(any(WeightRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(latest));

        weightService.addWeight(1L, 66.0, date, null, null, null);

        verify(weightRecordRepository, never()).deleteAll(any());
    }

    @Test
    void addWeight_newRecord_creates() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(weightRecordRepository.findByUserIdAndRecordedDateOrderByIdDesc(1L, date)).thenReturn(List.of());
        when(weightRecordRepository.save(any(WeightRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        WeightRecord created = rec(9L, 65.0, date);
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(created));

        WeightRecord saved = weightService.addWeight(1L, 65.0, date, 22.5, 80.0, 100.0);

        assertEquals(65.0, saved.getWeightKg());
        assertEquals(22.5, saved.getBodyFatPct());
        assertEquals(date, saved.getRecordedDate());
        assertEquals(1L, saved.getUser().getId());
        verify(userService).recalculateTargetsFromWeight(1L, 65.0);
    }

    @Test
    void addWeight_bodyComponents_onlyUpdatedWhenProvided() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        WeightRecord latest = WeightRecord.builder().id(2L).user(User.builder().id(1L).build())
                .weightKg(70.0).recordedDate(date).bodyFatPct(20.0).build();
        when(weightRecordRepository.findByUserIdAndRecordedDateOrderByIdDesc(1L, date)).thenReturn(List.of(latest));
        when(weightRecordRepository.save(any(WeightRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(latest));

        weightService.addWeight(1L, 69.0, date, null, null, 95.0); // 只传 hipCm

        assertEquals(69.0, latest.getWeightKg());
        assertEquals(20.0, latest.getBodyFatPct()); // 未传 → 保留原值
        assertEquals(95.0, latest.getHipCm());
        assertNull(latest.getWaistCm());
    }

    // ===== 最新体重 =====

    @Test
    void getLatestWeight_empty() {
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of());
        Map<String, Object> m = weightService.getLatestWeight(1L);
        assertNull(m.get("weightKg"));
        assertNull(m.get("diff"));
    }

    @Test
    void getLatestWeight_twoRecords_computesDiff() {
        LocalDate today = TimeUtil.today();
        WeightRecord latest = rec(2L, 70.0, today);
        WeightRecord prev = rec(1L, 69.0, today.minusDays(1));
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(latest, prev));

        Map<String, Object> m = weightService.getLatestWeight(1L);

        assertEquals(70.0, m.get("weightKg"));
        assertEquals(1.0, m.get("diff"));
    }

    @Test
    void getLatestWeight_singleRecord_noDiff() {
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(rec(2L, 70.0, TimeUtil.today())));
        Map<String, Object> m = weightService.getLatestWeight(1L);
        assertEquals(70.0, m.get("weightKg"));
        assertNull(m.get("diff"));
    }

    // ===== 历史 =====

    @Test
    void getWeightHistory_mapsPoints() {
        LocalDate today = TimeUtil.today();
        LocalDate start = today.minusDays(6);
        WeightRecord r = WeightRecord.builder().recordedDate(today).weightKg(65.0).bodyFatPct(20.0).waistCm(80.0).hipCm(100.0).build();
        when(weightRecordRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(1L, start, today))
                .thenReturn(List.of(r));

        List<WeightHistoryPoint> list = weightService.getWeightHistory(1L, 7);

        assertEquals(1, list.size());
        assertEquals(today, list.get(0).date());
        assertEquals(65.0, list.get(0).weightKg());
        assertEquals(20.0, list.get(0).bodyFatPct());
    }

    @Test
    void getRecords_passthrough() {
        when(weightRecordRepository.findByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(rec(1L, 70.0, TimeUtil.today())));
        assertEquals(1, weightService.getRecords(1L).size());
    }

    // ===== 更新 =====

    @Test
    void updateWeight_owner_updatesFields() {
        LocalDate newDate = LocalDate.of(2026, 8, 2);
        WeightRecord rec = rec(5L, 70.0, TimeUtil.today());
        when(weightRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        when(weightRecordRepository.save(any(WeightRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(rec));
        loginAs(1L);

        WeightRecord updated = weightService.updateWeight(5L, 68.0, newDate, 21.0, null, null);

        assertEquals(68.0, updated.getWeightKg());
        assertEquals(newDate, updated.getRecordedDate());
        assertEquals(21.0, updated.getBodyFatPct());
        verify(userService).recalculateTargetsFromWeight(1L, 68.0);
    }

    @Test
    void updateWeight_notFound_throws() {
        when(weightRecordRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> weightService.updateWeight(5L, 68.0, TimeUtil.today(), null, null, null));
    }

    @Test
    void updateWeight_wrongOwner_forbidden() {
        WeightRecord rec = rec(5L, 70.0, TimeUtil.today());
        when(weightRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        loginAs(2L);

        assertThrows(ForbiddenException.class,
                () -> weightService.updateWeight(5L, 68.0, TimeUtil.today(), null, null, null));
    }

    // ===== 删除 =====

    @Test
    void deleteWeight_owner_deletes() {
        WeightRecord rec = rec(5L, 70.0, TimeUtil.today());
        when(weightRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        when(weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(rec));
        loginAs(1L);

        weightService.deleteWeight(5L);

        verify(weightRecordRepository).delete(rec);
        verify(userService).recalculateTargetsFromWeight(1L, 70.0);
    }

    @Test
    void deleteWeight_notFound_throws() {
        when(weightRecordRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> weightService.deleteWeight(5L));
    }

    @Test
    void deleteWeight_wrongOwner_forbidden() {
        WeightRecord rec = rec(5L, 70.0, TimeUtil.today());
        when(weightRecordRepository.findById(5L)).thenReturn(Optional.of(rec));
        loginAs(2L);

        assertThrows(ForbiddenException.class, () -> weightService.deleteWeight(5L));
    }
}
