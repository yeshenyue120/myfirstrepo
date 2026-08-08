package com.example.calorieserver.controller;

import com.example.calorieserver.dto.WeightHistoryPoint;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.service.WeightService;
import com.example.calorieserver.util.ParamUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weights")
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    // 记录体重
    @PostMapping("/{userId}")
    public ResponseEntity<WeightRecord> addWeight(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        double weightKg = requirePositiveDouble(body, "weightKg", "体重");
        LocalDate date = requireDate(body, "recordedDate", "日期");
        Double bodyFatPct = toNullableDouble(body.get("bodyFatPct"), "体脂率");
        Double waistCm = toNullableDouble(body.get("waistCm"), "腰围");
        Double hipCm = toNullableDouble(body.get("hipCm"), "臀围");
        return ResponseEntity.ok(weightService.addWeight(userId, weightKg, date, bodyFatPct, waistCm, hipCm));
    }

    // 最新体重 + 差值
    @GetMapping("/latest/{userId}")
    public ResponseEntity<Map<String, Object>> getLatest(@PathVariable Long userId) {
        return ResponseEntity.ok(weightService.getLatestWeight(userId));
    }

    // 体重历史趋势
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<WeightHistoryPoint>> getHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        days = ParamUtil.clampDays(days);
        return ResponseEntity.ok(weightService.getWeightHistory(userId, days));
    }

    // 全部体重记录（带 id，供历史列表管理）
    @GetMapping("/{userId}")
    public ResponseEntity<List<WeightRecord>> getRecords(@PathVariable Long userId) {
        return ResponseEntity.ok(weightService.getRecords(userId));
    }

    // 更新单条体重记录
    @PutMapping("/record/{recordId}")
    public ResponseEntity<WeightRecord> updateRecord(
            @PathVariable Long recordId,
            @RequestBody Map<String, Object> body) {
        double weightKg = requirePositiveDouble(body, "weightKg", "体重");
        LocalDate date = body.get("recordedDate") != null && !body.get("recordedDate").toString().trim().isEmpty()
                ? requireDate(body, "recordedDate", "日期")
                : null;
        Double bodyFatPct = toNullableDouble(body.get("bodyFatPct"), "体脂率");
        Double waistCm = toNullableDouble(body.get("waistCm"), "腰围");
        Double hipCm = toNullableDouble(body.get("hipCm"), "臀围");
        return ResponseEntity.ok(weightService.updateWeight(recordId, weightKg, date, bodyFatPct, waistCm, hipCm));
    }

    // 删除单条体重记录
    @DeleteMapping("/record/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long recordId) {
        weightService.deleteWeight(recordId);
        return ResponseEntity.noContent().build();
    }

    // 必填正数解析（缺失/空/非法/非正 → 友好提示）
    private double requirePositiveDouble(Map<String, Object> body, String key, String label) {
        double value = requireDouble(body, key, label);
        if (value <= 0) {
            throw new BusinessException(label + "必须大于 0");
        }
        return value;
    }

    // 必填数值解析（缺失/空/非法 → 友好提示）
    private double requireDouble(Map<String, Object> body, String key, String label) {
        Object o = body.get(key);
        if (o == null || o.toString().trim().isEmpty()) {
            throw new BusinessException("请填写" + label);
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    // 必填日期解析
    private LocalDate requireDate(Map<String, Object> body, String key, String label) {
        Object o = body.get(key);
        if (o == null || o.toString().trim().isEmpty()) {
            throw new BusinessException("请选择" + label);
        }
        try {
            return LocalDate.parse(o.toString());
        } catch (Exception e) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    // 可选数值解析（缺省/空串 → null，负数/非法 → 友好提示）
    private Double toNullableDouble(Object o, String label) {
        if (o == null || o.toString().trim().isEmpty()) return null;
        try {
            double v = Double.parseDouble(o.toString());
            if (v < 0) {
                throw new BusinessException(label + "不能为负数");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new BusinessException(label + "格式不正确");
        }
    }
}