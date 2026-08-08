package com.example.calorieserver.controller;

import com.example.calorieserver.service.StatsService;
import com.example.calorieserver.util.ParamUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 打卡连续天数统计
     * GET /api/stats/{userId}/streak
     */
    @GetMapping("/{userId}/streak")
    public ResponseEntity<Map<String, Object>> getStreak(@PathVariable Long userId) {
        return ResponseEntity.ok(statsService.getStreak(userId));
    }

    /**
     * 周报/月报汇总
     * GET /api/stats/{userId}/report?endDate=2026-08-04&days=7
     */
    @GetMapping("/{userId}/report")
    public ResponseEntity<Map<String, Object>> getReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") int days) {
        days = ParamUtil.clampDays(days);
        return ResponseEntity.ok(statsService.getReport(userId, endDate, days));
    }
}
