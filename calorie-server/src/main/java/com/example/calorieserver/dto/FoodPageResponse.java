package com.example.calorieserver.dto;

import com.example.calorieserver.entity.Food;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 食物库分页响应。
 * items: 当前页食物；total: 总条数；hasMore: 是否还有下一页
 */
@Data
@AllArgsConstructor
public class FoodPageResponse {
    private List<Food> items;
    private long total;
    private boolean hasMore;
}
