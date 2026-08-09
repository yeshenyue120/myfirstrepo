package com.example.calorieserver.dto;

import com.example.calorieserver.entity.Food;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 食物库分页响应。
 * items: 当前页食物；total: 总条数；hasMore: 是否还有下一页
 * 需要 @NoArgsConstructor：Redis 缓存命中反序列化时需无参构造重建对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodPageResponse {
    private List<Food> items;
    private long total;
    private boolean hasMore;
}
