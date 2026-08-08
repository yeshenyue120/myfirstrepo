package com.example.calorieserver.controller;

import com.example.calorieserver.dto.ExerciseRequest;
import com.example.calorieserver.entity.ExerciseRecord;
import com.example.calorieserver.service.ExerciseService;
import com.example.calorieserver.util.ParamUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    /**
     * 新增运动记录
     * POST /api/exercises/{userId}?weightKg=65
     */
    @PostMapping("/{userId}")
    public ResponseEntity<ExerciseRecord> addExercise(
            @PathVariable Long userId,
            @RequestParam double weightKg,
            @Valid @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.addExercise(userId, request, weightKg));
    }

    /**
     * 查询某天运动记录
     * GET /api/exercises/{userId}?date=2026-08-01
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<ExerciseRecord>> getByDate(
            @PathVariable Long userId,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(exerciseService.getByDate(userId, date));
    }

    /**
     * 某天运动汇总（不传 date 默认今天）
     * GET /api/exercises/{userId}/summary?date=2026-08-01
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exerciseService.getSummary(userId, date));
    }

    /**
     * 更新运动记录
     * PUT /api/exercises/record/{recordId}?weightKg=65
     */
    @PutMapping("/record/{recordId}")
    public ResponseEntity<ExerciseRecord> updateExercise(
            @PathVariable Long recordId,
            @RequestParam double weightKg,
            @Valid @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.updateExercise(recordId, request, weightKg));
    }

    /**
     * 删除运动记录
     * DELETE /api/exercises/record/{recordId}
     */
    @DeleteMapping("/record/{recordId}")
    public ResponseEntity<Void> deleteExercise(@PathVariable Long recordId) {
        exerciseService.deleteExercise(recordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 近 N 天运动趋势
     * GET /api/exercises/{userId}/trend?endDate=2026-08-04&days=7
     */
    @GetMapping("/{userId}/trend")
    public ResponseEntity<Map<LocalDate, Double>> getExerciseTrend(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") int days) {
        days = ParamUtil.clampDays(days);
        return ResponseEntity.ok(exerciseService.getExerciseTrend(userId, endDate, days));
    }

    /**
     * 最近使用过的运动类型（去重倒序）
     * GET /api/exercises/{userId}/recent-types
     */
    @GetMapping("/{userId}/recent-types")
    public ResponseEntity<List<String>> getRecentExerciseTypes(@PathVariable Long userId) {
        return ResponseEntity.ok(exerciseService.getRecentExerciseTypes(userId));
    }
}
