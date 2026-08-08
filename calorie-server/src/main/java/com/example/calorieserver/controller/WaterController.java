package com.example.calorieserver.controller;

import com.example.calorieserver.entity.WaterRecord;
import com.example.calorieserver.service.WaterService;
import com.example.calorieserver.util.ParamUtil;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/water")
@RequiredArgsConstructor
@Validated
public class WaterController {

    private final WaterService waterService;

    /**
     * 新增喝水记录
     * POST /api/water/{userId}?date=2026-08-03&amountMl=250
     */
    @PostMapping("/{userId}")
    public ResponseEntity<WaterRecord> addWater(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @Positive(message = "喝水量必须大于 0") int amountMl) {
        return ResponseEntity.ok(waterService.addWater(userId, date, amountMl));
    }

    /**
     * 查询某天喝水记录
     * GET /api/water/{userId}?date=2026-08-03
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<WaterRecord>> getByDate(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(waterService.getByDate(userId, date));
    }

    /**
     * 某天喝水汇总（不传 date 默认今天）
     * GET /api/water/{userId}/summary?date=2026-08-03
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(waterService.getSummary(userId, date));
    }

    /**
     * 近 N 天喝水量趋势
     * GET /api/water/{userId}/trend?endDate=2026-08-03&days=7
     */
    @GetMapping("/{userId}/trend")
    public ResponseEntity<Map<LocalDate, Integer>> getTrend(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") int days) {
        days = ParamUtil.clampDays(days);
        return ResponseEntity.ok(waterService.getTrend(userId, endDate, days));
    }

    /**
     * 更新喝水记录
     * PUT /api/water/record/{recordId}?amountMl=300
     */
    @PutMapping("/record/{recordId}")
    public ResponseEntity<WaterRecord> updateWater(
            @PathVariable Long recordId,
            @RequestParam @Positive(message = "喝水量必须大于 0") int amountMl) {
        return ResponseEntity.ok(waterService.updateWater(recordId, amountMl));
    }

    /**
     * 删除喝水记录
     * DELETE /api/water/record/{recordId}
     */
    @DeleteMapping("/record/{recordId}")
    public ResponseEntity<Void> deleteWater(@PathVariable Long recordId) {
        waterService.deleteWater(recordId);
        return ResponseEntity.noContent().build();
    }
}
