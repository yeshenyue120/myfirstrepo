package com.example.calorieserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRequest {
    @NotBlank(message = "请选择运动类型")
    private String exerciseType;

    @NotNull(message = "请填写运动时长")
    @Positive(message = "运动时长需大于 0")
    private Integer durationMin;

    @NotNull(message = "运动强度数据缺失")
    @Positive(message = "运动强度数据异常")
    private Double metValue;

    private String recordDate;
}
