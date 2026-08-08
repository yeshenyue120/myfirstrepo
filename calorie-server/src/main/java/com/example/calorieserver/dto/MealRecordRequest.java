package com.example.calorieserver.dto;

import com.example.calorieserver.entity.MealRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MealRecordRequest {

    @NotNull(message = "请选择食物")
    private Long foodId;

    @NotNull(message = "请填写克数")
    @Positive(message = "克数必须大于 0")
    private Double grams;

    @NotNull(message = "请选择餐次")
    private MealRecord.MealType mealType;

    @NotNull(message = "请选择日期")
    private LocalDate mealDate;

    private String note;
}