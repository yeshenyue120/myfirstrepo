package com.example.calorieserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateFoodRequest {

    @NotBlank(message = "食物名称不能为空")
    private String name;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotNull(message = "热量不能为空")
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbsPer100g;
}
