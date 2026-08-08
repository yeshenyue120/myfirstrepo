package com.example.calorieserver.dto;

import com.example.calorieserver.entity.MealRecord;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MealRecordResponse {

    private Long id;
    private Long foodId;
    private String foodName;
    private Double grams;
    private Double totalCalories;
    private String mealType;
    private LocalDate mealDate;
    private String note;
    private LocalDateTime createdAt;

    public static MealRecordResponse fromEntity(MealRecord record) {
        return MealRecordResponse.builder()
                .id(record.getId())
                .foodId(record.getFood().getId())
                .foodName(record.getFood().getName())
                .grams(record.getGrams())
                .totalCalories(record.getTotalCalories())
                .mealType(record.getMealType().name())
                .mealDate(record.getMealDate())
                .note(record.getNote())
                .createdAt(record.getCreatedAt())
                .build();
    }
}