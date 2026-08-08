package com.example.calorieserver.controller;

import com.example.calorieserver.entity.SleepRecord;
import com.example.calorieserver.service.SleepService;
import com.example.calorieserver.util.ParamUtil;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sleep")
@RequiredArgsConstructor
@Validated
public class SleepController {

    private final SleepService sleepService;

    /**
     * 新增/覆盖当天睡眠记录
     * POST /api/sleep/{userId}?date=2026-08-03&durationMin=480
     */
    @PostMapping("/{userId}")
    public ResponseEntity<SleepRecord> saveSleep(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @Positive(message = "睡眠时长必须大于 0") int durationMin) {
        return ResponseEntity.ok(sleepService.saveSleep(userId, date, durationMin));
    }

    /**
     * 查询某天睡眠记录
     * GET /api/sleep/{userId}?date=2026-08-03
     */
    @GetMapping("/{userId}")
    public ResponseEntity<SleepRecord> getByDate(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sleepService.getByDate(userId, date));
    }

    /**
     * 某天睡眠汇总（不传 date 默认今天）
     * GET /api/sleep/{userId}/summary?date=2026-08-03
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sleepService.getSummary(userId, date));
    }

    /**
     * 近 N 天睡眠时长趋势
     * GET /api/sleep/{userId}/trend?endDate=2026-08-03&days=7
     */
    @GetMapping("/{userId}/trend")
    public ResponseEntity<Map<LocalDate, Integer>> getTrend(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") int days) {
        days = ParamUtil.clampDays(days);
        return ResponseEntity.ok(sleepService.getTrend(userId, endDate, days));
    }

    /**
     * 更新睡眠记录
     * PUT /api/sleep/record/{recordId}?durationMin=420
     */
    @PutMapping("/record/{recordId}")
    public ResponseEntity<SleepRecord> updateSleep(
            @PathVariable Long recordId,
            @RequestParam @Positive(message = "睡眠时长必须大于 0") int durationMin) {
        return ResponseEntity.ok(sleepService.updateSleep(recordId, durationMin));
    }

    /**
     * 删除睡眠记录
     * DELETE /api/sleep/record/{recordId}
     */
    @DeleteMapping("/record/{recordId}")
    public ResponseEntity<Void> deleteSleep(@PathVariable Long recordId) {
        sleepService.deleteSleep(recordId);
        return ResponseEntity.noContent().build();
    }
}
