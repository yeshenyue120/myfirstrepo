package com.example.calorieserver.controller;

import com.example.calorieserver.dto.CreateFoodRequest;
import com.example.calorieserver.dto.FoodPageResponse;
import com.example.calorieserver.entity.Food;
import com.example.calorieserver.entity.FoodCategory;
import com.example.calorieserver.service.FoodService;
import com.example.calorieserver.util.ParamUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    // ===== 分类 =====
    @GetMapping("/categories")
    public ResponseEntity<List<FoodCategory>> getAllCategories() {
        return ResponseEntity.ok(foodService.getAllCategories());
    }

    // ===== 食物查询 =====

    // 查常见食物
    @GetMapping("/common")
    public ResponseEntity<List<Food>> getCommonFoods() {
        return ResponseEntity.ok(foodService.getCommonFoods());
    }

    // 按分类查
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Food>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(foodService.getFoodsByCategory(categoryId));
    }

    // 按分类查常见食物（分类页只展示常见）
    @GetMapping("/category/{categoryId}/common")
    public ResponseEntity<List<Food>> getCommonByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(foodService.getCommonFoodsByCategory(categoryId));
    }

    // 关键词搜索
    @GetMapping("/search")
    public ResponseEntity<List<Food>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(foodService.searchFoods(keyword));
    }

    // ===== 食物库分页（全量浏览） =====

    // 全库公共食物分页
    @GetMapping("/library")
    public ResponseEntity<FoodPageResponse> getFoodLibrary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        page = ParamUtil.clampPage(page);
        size = ParamUtil.clampSize(size);
        return ResponseEntity.ok(foodService.getFoodLibraryPage(page, size));
    }

    // 全库关键词搜索分页
    @GetMapping("/search/page")
    public ResponseEntity<FoodPageResponse> searchFoodsPage(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        page = ParamUtil.clampPage(page);
        size = ParamUtil.clampSize(size);
        return ResponseEntity.ok(foodService.searchFoodsPage(keyword, page, size));
    }

    // ===== 收藏 =====
    // 注：userId 暂时从请求参数传，后面加 JWT 后从 Token 获取

    @PostMapping("/favorite")
    public ResponseEntity<Void> addFavorite(@RequestParam Long userId, @RequestParam Long foodId) {
        foodService.addFavorite(userId, foodId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorite")
    public ResponseEntity<Void> removeFavorite(@RequestParam Long userId, @RequestParam Long foodId) {
        foodService.removeFavorite(userId, foodId);
        return ResponseEntity.noContent().build();
    }

    // 收藏的食物列表（返回 Food 对象）
    @GetMapping("/favorites/{userId}")
    public ResponseEntity<List<Food>> getFavoriteFoods(@PathVariable Long userId) {
        return ResponseEntity.ok(foodService.getFavoriteFoods(userId));
    }

    // 用户收藏的食物 ID 列表（前端判断收藏状态）
    @GetMapping("/favorites/ids/{userId}")
    public ResponseEntity<List<Long>> getFavoriteFoodIds(@PathVariable Long userId) {
        return ResponseEntity.ok(foodService.getFavoriteFoodIds(userId));
    }

    // ===== 自定义食物 =====

    // 创建自定义食物
    @PostMapping("/custom")
    public ResponseEntity<Food> createCustomFood(@RequestParam Long userId, @Valid @RequestBody CreateFoodRequest request) {
        return ResponseEntity.ok(foodService.createCustomFood(userId, request));
    }

    // 删除自定义食物
    @DeleteMapping("/custom/{foodId}")
    public ResponseEntity<Void> deleteCustomFood(@PathVariable Long foodId, @RequestParam Long userId) {
        foodService.deleteCustomFood(foodId, userId);
        return ResponseEntity.noContent().build();
    }

    // 用户自定义食物
    @GetMapping("/creator/{userId}")
    public ResponseEntity<List<Food>> getUserFoods(@PathVariable Long userId) {
        return ResponseEntity.ok(foodService.getUserFoods(userId));
    }

}