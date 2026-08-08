package com.example.calorieserver.service;

import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WaterRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.WaterRecordRepository;
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
 * WaterService 单元测试：新增/查询/汇总/趋势/增删（含归属校验）。
 */
@ExtendWith(MockitoExtension.class)
class WaterServiceTest {

    @Mock
    private WaterRecordRepository waterRecordRepository;

    @InjectMocks
    private WaterService waterService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void addWater_buildsRecord() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(waterRecordRepository.save(any(WaterRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        WaterRecord saved = waterService.addWater(1L, date, 300);

        assertEquals(date, saved.getDate());
        assertEquals(300, saved.getAmountMl());
        assertEquals(1L, saved.getUser().getId());
    }

    @Test
    void getSummary_totalsAndCount() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        WaterRecord rec = WaterRecord.builder().amountMl(250).date(date).build();
        when(waterRecordRepository.findByUserIdAndDateOrderByIdDesc(1L, date)).thenReturn(List.of(rec));
        when(waterRecordRepository.sumMlByUserIdAndDate(1L, date)).thenReturn(800);

        Map<String, Object> s = waterService.getSummary(1L, date);

        assertEquals(800, s.get("totalMl"));
        assertEquals(1, s.get("count"));
        assertEquals("2026-08-01", s.get("date"));
    }

    @Test
    void getSummary_nullDate_usesToday_nullTotalBecomesZero() {
        when(waterRecordRepository.findByUserIdAndDateOrderByIdDesc(1L, TimeUtil.today())).thenReturn(List.of());
        when(waterRecordRepository.sumMlByUserIdAndDate(1L, TimeUtil.today())).thenReturn(null);

        Map<String, Object> s = waterService.getSummary(1L, null);

        assertEquals(0, s.get("totalMl"));
        assertEquals(TimeUtil.today().toString(), s.get("date"));
    }

    @Test
    void getTrend_mapsRows() {
        LocalDate end = LocalDate.of(2026, 8, 7);
        when(waterRecordRepository.sumMlGroupByDate(1L, LocalDate.of(2026, 8, 1), end))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 8, 1), 500}));

        Map<LocalDate, Integer> trend = waterService.getTrend(1L, end, 7);

        assertEquals(500, trend.get(LocalDate.of(2026, 8, 1)));
    }

    @Test
    void updateWater_ownerUpdates() {
        WaterRecord rec = WaterRecord.builder().id(3L).user(User.builder().id(1L).build()).amountMl(200).build();
        when(waterRecordRepository.findById(3L)).thenReturn(Optional.of(rec));
        when(waterRecordRepository.save(any(WaterRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAs(1L);

        WaterRecord updated = waterService.updateWater(3L, 500);

        assertEquals(500, updated.getAmountMl());
    }

    @Test
    void updateWater_notFound_throws() {
        when(waterRecordRepository.findById(3L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> waterService.updateWater(3L, 500));
    }

    @Test
    void updateWater_wrongOwner_forbidden() {
        WaterRecord rec = WaterRecord.builder().id(3L).user(User.builder().id(1L).build()).build();
        when(waterRecordRepository.findById(3L)).thenReturn(Optional.of(rec));
        loginAs(2L);

        assertThrows(ForbiddenException.class, () -> waterService.updateWater(3L, 500));
    }

    @Test
    void deleteWater_ownerDeletes() {
        WaterRecord rec = WaterRecord.builder().id(3L).user(User.builder().id(1L).build()).build();
        when(waterRecordRepository.findById(3L)).thenReturn(Optional.of(rec));
        loginAs(1L);

        waterService.deleteWater(3L);

        verify(waterRecordRepository).delete(rec);
    }

    @Test
    void deleteWater_notFound_throws() {
        when(waterRecordRepository.findById(3L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> waterService.deleteWater(3L));
    }
}
