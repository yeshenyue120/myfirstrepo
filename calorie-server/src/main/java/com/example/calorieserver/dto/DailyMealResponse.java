package com.example.calorieserver.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DailyMealResponse {

    private LocalDate mealDate;
    private Double totalCalories;
    private Map<String, List<MealRecordResponse>> meals;  // 按餐次分组
}