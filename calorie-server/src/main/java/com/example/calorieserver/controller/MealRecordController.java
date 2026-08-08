package com.example.calorieserver.controller;

import com.example.calorieserver.dto.DailyMealResponse;
import com.example.calorieserver.dto.DailyNutritionResponse;
import com.example.calorieserver.dto.MealRecordRequest;
import com.example.calorieserver.dto.MealRecordResponse;
import com.example.calorieserver.entity.MealRecord;
import com.example.calorieserver.service.MealRecordService;
import com.example.calorieserver.util.ParamUtil;
import com.example.calorieserver.util.TimeUtil;
import jakarta.validation.Valid;
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
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Validated
public class MealRecordController {

    private final MealRecordService mealRecordService;

    // 添加记录
    @PostMapping("/{userId}")
    public ResponseEntity<MealRecordResponse> addRecord(
            @PathVariable Long userId,
            @Valid @RequestBody MealRecordRequest request) {
        return ResponseEntity.ok(mealRecordService.addRecord(userId, request));
    }

    // 批量添加记录
    @PostMapping("/batch/{userId}")
    public ResponseEntity<List<MealRecordResponse>> batchAddRecords(
            @PathVariable Long userId,
            @Valid @RequestBody List<@Valid MealRecordRequest> requests) {
        return ResponseEntity.ok(mealRecordService.batchAddRecords(userId, requests));
    }

    // 复制昨天某一餐的记录
    @GetMapping("/{userId}/yesterday")
    public ResponseEntity<List<MealRecordResponse>> getYesterdayMeal(
            @PathVariable Long userId,
            @RequestParam String mealType) {
        return ResponseEntity.ok(mealRecordService.getYesterdayMeal(userId, mealType));
    }

    // 查某天所有记录
    @GetMapping("/{userId}")
    public ResponseEntity<List<MealRecordResponse>> getByDate(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(mealRecordService.getRecordsByDate(userId, date));
    }

    // 查某天某一餐
    @GetMapping("/{userId}/meal")
    public ResponseEntity<List<MealRecordResponse>> getByDateAndMeal(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam MealRecord.MealType mealType) {
        return ResponseEntity.ok(mealRecordService.getRecordsByDateAndMeal(userId, date, mealType));
    }

    // 修改记录克数
    @PutMapping("/{recordId}")
    public ResponseEntity<MealRecordResponse> updateRecord(
            @PathVariable Long recordId,
            @RequestParam @Positive(message = "克数必须大于 0") Double grams) {
        return ResponseEntity.ok(mealRecordService.updateRecord(recordId, grams));
    }

    // 删除单次记录
    @DeleteMapping("/{recordId}")
    public ResponseEntity<String> deleteRecord(@PathVariable Long recordId) {
        mealRecordService.deleteRecord(recordId);
        return ResponseEntity.ok("删除成功");
    }

    // 清空某天所有记录
    @DeleteMapping("/{userId}/date")
    public ResponseEntity<String> deleteByDate(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        mealRecordService.deleteRecordsByDate(userId, date);
        return ResponseEntity.ok("删除成功");
    }

    // 某天总热量
    @GetMapping("/{userId}/total")
    public ResponseEntity<Double> getTotalCalories(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(mealRecordService.getTotalCalories(userId, date));
    }


    // 查某天记录（按餐次分组），不传 date 默认今天
    @GetMapping("/summary/{userId}")
    public ResponseEntity<DailyMealResponse> getDailySummary(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = TimeUtil.today();
        }
        return ResponseEntity.ok(mealRecordService.getDailySummary(userId, date));
    }

    // 近 N 天趋势（默认 7 天）
    @GetMapping("/{userId}/trend")
    public ResponseEntity<Map<LocalDate, Double>> getWeeklyTrend(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") int days) {
        days = ParamUtil.clampDays(days);
        return ResponseEntity.ok(mealRecordService.getWeeklyTrend(userId, endDate, days));
    }

    // 每日营养汇总（蛋白质/脂肪/碳水）
    @GetMapping("/nutrition/{userId}")
    public ResponseEntity<DailyNutritionResponse> getDailyNutrition(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(mealRecordService.getDailyNutrition(userId, date));
    }
}