package com.example.calorieserver.service;

import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.WeightRecordRepository;
import com.example.calorieserver.security.SecurityUtil;
import com.example.calorieserver.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.calorieserver.dto.WeightHistoryPoint;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeightService {

    private final WeightRecordRepository weightRecordRepository;
    private final UserService userService;

    // 记录体重（同一天有记录则覆盖，清理历史重复数据）
    @Transactional
    public WeightRecord addWeight(Long userId, double weightKg, LocalDate date,
                                  Double bodyFatPct, Double waistCm, Double hipCm) {
        List<WeightRecord> existing = weightRecordRepository
                .findByUserIdAndRecordedDateOrderByIdDesc(userId, date);

        if (!existing.isEmpty()) {
            // 更新最新那条
            WeightRecord latest = existing.get(0);
            latest.setWeightKg(weightKg);
            // 体成分只在传值时更新，避免仅记体重时把已有值覆盖掉
            if (bodyFatPct != null) latest.setBodyFatPct(bodyFatPct);
            if (waistCm != null) latest.setWaistCm(waistCm);
            if (hipCm != null) latest.setHipCm(hipCm);
            weightRecordRepository.save(latest);
            // 删掉多余的旧记录
            if (existing.size() > 1) {
                weightRecordRepository.deleteAll(existing.subList(1, existing.size()));
            }
            recalculateTargets(userId);
            return latest;
        }

        WeightRecord record = WeightRecord.builder()
                .user(User.builder().id(userId).build())
                .weightKg(weightKg)
                .recordedDate(date)
                .bodyFatPct(bodyFatPct)
                .waistCm(waistCm)
                .hipCm(hipCm)
                .build();
        WeightRecord saved = weightRecordRepository.save(record);
        recalculateTargets(userId);
        return saved;
    }

    public Map<String, Object> getLatestWeight(Long userId) {
        List<WeightRecord> records = weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(userId);
        Map<String, Object> result = new HashMap<>();
        if (records.isEmpty()) {
            result.put("weightKg", null);
            result.put("diff", null);
        } else {
            WeightRecord latest = records.get(0);
            result.put("weightKg", latest.getWeightKg());
            if (records.size() > 1) {
                double diff = Math.round((latest.getWeightKg() - records.get(1).getWeightKg()) * 10.0) / 10.0;
                result.put("diff", diff);
            } else {
                result.put("diff", null);
            }
        }
        return result;
    }

    // 查体重历史（趋势图用）
    public List<WeightHistoryPoint> getWeightHistory(Long userId, int days) {
        LocalDate endDate = TimeUtil.today();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<WeightRecord> records = weightRecordRepository
                .findByUserIdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(userId, startDate, endDate);
        return records.stream()
                .map(r -> new WeightHistoryPoint(r.getRecordedDate(), r.getWeightKg(),
                        r.getBodyFatPct(), r.getWaistCm(), r.getHipCm()))
                .collect(Collectors.toList());
    }

    // 查全部体重记录（带 id，供历史列表管理）
    public List<WeightRecord> getRecords(Long userId) {
        return weightRecordRepository.findByUserIdOrderByRecordedDateDescIdDesc(userId);
    }

    // 更新单条体重记录
    public WeightRecord updateWeight(Long recordId, double weightKg, LocalDate date,
                                     Double bodyFatPct, Double waistCm, Double hipCm) {
        WeightRecord record = weightRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("体重记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        record.setWeightKg(weightKg);
        if (date != null) {
            record.setRecordedDate(date);
        }
        if (bodyFatPct != null) record.setBodyFatPct(bodyFatPct);
        if (waistCm != null) record.setWaistCm(waistCm);
        if (hipCm != null) record.setHipCm(hipCm);
        WeightRecord saved = weightRecordRepository.save(record);
        recalculateTargets(record.getUser().getId());
        return saved;
    }

    // 删除单条体重记录
    public void deleteWeight(Long recordId) {
        WeightRecord record = weightRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("体重记录不存在"));
        SecurityUtil.requireOwner(record.getUser().getId());
        weightRecordRepository.delete(record);
        recalculateTargets(record.getUser().getId());
    }

    // 体重记录变化后：取最新一条体重重算目标（无记录则保持现状）
    private void recalculateTargets(Long userId) {
        List<WeightRecord> top = weightRecordRepository.findTop2ByUserIdOrderByRecordedDateDescIdDesc(userId);
        if (top.isEmpty()) return;
        userService.recalculateTargetsFromWeight(userId, top.get(0).getWeightKg());
    }
}