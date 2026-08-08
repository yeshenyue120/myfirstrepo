package com.example.calorieserver.service;

import com.example.calorieserver.entity.SleepRecord;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.SleepRecordRepository;
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
 * SleepService 单元测试：同天覆盖/新增、查询、汇总、趋势、增删（含归属校验）。
 */
@ExtendWith(MockitoExtension.class)
class SleepServiceTest {

    @Mock
    private SleepRecordRepository sleepRecordRepository;

    @InjectMocks
    private SleepService sleepService;

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
    void saveSleep_existingOverwrites() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        SleepRecord existing = SleepRecord.builder().id(1L).date(date).durationMin(420).build();
        when(sleepRecordRepository.findByUserIdAndDate(1L, date)).thenReturn(Optional.of(existing));
        when(sleepRecordRepository.save(any(SleepRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        SleepRecord saved = sleepService.saveSleep(1L, date, 450);

        assertEquals(450, saved.getDurationMin());
        verify(sleepRecordRepository, never()).save(argThat(r -> r.getId() == null)); // 走更新而非新增
    }

    @Test
    void saveSleep_newCreates() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(sleepRecordRepository.findByUserIdAndDate(1L, date)).thenReturn(Optional.empty());
        when(sleepRecordRepository.save(any(SleepRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        SleepRecord saved = sleepService.saveSleep(1L, date, 480);

        assertEquals(480, saved.getDurationMin());
        assertEquals(date, saved.getDate());
        assertEquals(1L, saved.getUser().getId());
    }

    @Test
    void getByDate_presentAndAbsent() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        when(sleepRecordRepository.findByUserIdAndDate(1L, date)).thenReturn(Optional.of(SleepRecord.builder().build()));
        assertNotNull(sleepService.getByDate(1L, date));

        when(sleepRecordRepository.findByUserIdAndDate(1L, date)).thenReturn(Optional.empty());
        assertNull(sleepService.getByDate(1L, date));
    }

    @Test
    void getSummary_recordPresent() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        SleepRecord rec = SleepRecord.builder().durationMin(450).build();
        when(sleepRecordRepository.findByUserIdAndDate(1L, date)).thenReturn(Optional.of(rec));
        when(sleepRecordRepository.sumMinByUserIdAndDate(1L, date)).thenReturn(450);

        Map<String, Object> s = sleepService.getSummary(1L, date);

        assertEquals(450, s.get("durationMin"));
        assertEquals(450, s.get("totalMin"));
        assertEquals("2026-08-01", s.get("date"));
    }

    @Test
    void getSummary_noRecord_durationZero() {
        when(sleepRecordRepository.findByUserIdAndDate(1L, TimeUtil.today())).thenReturn(Optional.empty());
        when(sleepRecordRepository.sumMinByUserIdAndDate(1L, TimeUtil.today())).thenReturn(null);

        Map<String, Object> s = sleepService.getSummary(1L, null);

        assertEquals(0, s.get("durationMin"));
        assertEquals(0, s.get("totalMin"));
    }

    @Test
    void getTrend_mapsRows() {
        LocalDate end = LocalDate.of(2026, 8, 7);
        when(sleepRecordRepository.sumMinGroupByDate(1L, LocalDate.of(2026, 8, 1), end))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 8, 1), 420}));

        Map<LocalDate, Integer> trend = sleepService.getTrend(1L, end, 7);

        assertEquals(420, trend.get(LocalDate.of(2026, 8, 1)));
    }

    @Test
    void updateSleep_ownerUpdates() {
        SleepRecord rec = SleepRecord.builder().id(4L).user(User.builder().id(1L).build()).durationMin(300).build();
        when(sleepRecordRepository.findById(4L)).thenReturn(Optional.of(rec));
        when(sleepRecordRepository.save(any(SleepRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        loginAs(1L);

        SleepRecord updated = sleepService.updateSleep(4L, 400);

        assertEquals(400, updated.getDurationMin());
    }

    @Test
    void updateSleep_wrongOwner_forbidden() {
        SleepRecord rec = SleepRecord.builder().id(4L).user(User.builder().id(1L).build()).build();
        when(sleepRecordRepository.findById(4L)).thenReturn(Optional.of(rec));
        loginAs(2L);

        assertThrows(ForbiddenException.class, () -> sleepService.updateSleep(4L, 400));
    }

    @Test
    void deleteSleep_ownerDeletes() {
        SleepRecord rec = SleepRecord.builder().id(4L).user(User.builder().id(1L).build()).build();
        when(sleepRecordRepository.findById(4L)).thenReturn(Optional.of(rec));
        loginAs(1L);

        sleepService.deleteSleep(4L);

        verify(sleepRecordRepository).delete(rec);
    }
}
