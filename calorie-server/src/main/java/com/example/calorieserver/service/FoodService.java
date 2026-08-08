package com.example.calorieserver.service;

import com.example.calorieserver.dto.CreateFoodRequest;
import com.example.calorieserver.dto.FoodPageResponse;
import com.example.calorieserver.entity.*;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final MealRecordRepository mealRecordRepository;

    // ===== 分类相关 =====

    // 查所有分类
    @Cacheable(cacheNames = "foodCategories")
    public List<FoodCategory> getAllCategories() {
        return foodCategoryRepository.findAll();
    }

    // ===== 食物查询 =====

    // 查常见食物
    @Cacheable(cacheNames = "foodCommon")
    public List<Food> getCommonFoods() {
        return foodRepository.findByIsPublicTrueAndIsCommonTrue();
    }

    // 按分类查
    @Cacheable(cacheNames = "foodCategory", key = "#categoryId")
    public List<Food> getFoodsByCategory(Long categoryId) {
        return foodRepository.findByCategoryIdAndIsPublicTrueOrderByCaloriesPer100gAsc(categoryId);
    }

    // 按分类查常见食物（分类页只展示常见）
    @Cacheable(cacheNames = "foodCategoryCommon", key = "#categoryId")
    public List<Food> getCommonFoodsByCategory(Long categoryId) {
        return foodRepository.findByCategoryIdAndIsPublicTrueAndIsCommonTrueOrderByCaloriesPer100gAsc(categoryId);
    }

    // 关键词搜索（公共库过滤，按卡路里升序排列；空关键词返回空列表，避免 LIKE '%%' 拉全库）
    public List<Food> searchFoods(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return foodRepository.findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc(keyword.trim());
    }

    // ===== 分页（食物库全量浏览） =====

    // 全库公共食物分页
    @Cacheable(cacheNames = "foodLibrary", key = "#page + '-' + #size")
    public FoodPageResponse getFoodLibraryPage(int page, int size) {
        Page<Food> result = foodRepository.findByIsPublicTrueOrderByCaloriesPer100gAsc(PageRequest.of(page, size));
        return toPageResponse(result);
    }

    // 全库关键词搜索分页
    @Cacheable(cacheNames = "foodSearchPage", key = "#keyword + ':' + #page + ':' + #size")
    public FoodPageResponse searchFoodsPage(String keyword, int page, int size) {
        Page<Food> result = foodRepository
                .findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc(keyword, PageRequest.of(page, size));
        return toPageResponse(result);
    }

    private FoodPageResponse toPageResponse(Page<Food> page) {
        return new FoodPageResponse(page.getContent(), page.getTotalElements(), page.hasNext());
    }

    // ===== 收藏相关 =====

    // 收藏食物
    @Transactional
    public void addFavorite(Long userId, Long foodId) {
        // 检查是否已收藏
        if (userFavoriteRepository.findByUserIdAndFoodId(userId, foodId).isPresent()) {
            throw new BusinessException("已收藏该食物");
        }
        // 冗余存菜名，方便直接查表
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new BusinessException("食物不存在: " + foodId));
        UserFavorite favorite = UserFavorite.builder()
                .user(User.builder().id(userId).build())
                .food(Food.builder().id(foodId).build())
                .foodName(food.getName())
                .build();
        userFavoriteRepository.save(favorite);
    }

    // 取消收藏
    @Transactional
    public void removeFavorite(Long userId, Long foodId) {
        userFavoriteRepository.deleteByUserIdAndFoodId(userId, foodId);
    }

    // 查用户收藏列表（返回 Food 列表）
    public List<Food> getFavoriteFoods(Long userId) {
        List<UserFavorite> favorites = userFavoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(UserFavorite::getFood)
                .toList();
    }

    // 查用户自定义食物
    public List<Food> getUserFoods(Long userId) {
        return foodRepository.findByCreatorId(userId);
    }

    // 是否已收藏
    public boolean isFavorited(Long userId, Long foodId) {
        return userFavoriteRepository.findByUserIdAndFoodId(userId, foodId).isPresent();
    }

    // 查用户收藏的食物 ID 列表（前端判断收藏状态）
    public List<Long> getFavoriteFoodIds(Long userId) {
        return userFavoriteRepository.findByUserId(userId).stream()
                .map(uf -> uf.getFood().getId())
                .toList();
    }

    // ===== 自定义食物 =====

    // 创建自定义食物（isPublic=false, isCommon=false, creator=当前用户）
    @Transactional
    public Food createCustomFood(Long userId, CreateFoodRequest request) {
        // 检查名称是否重复
        if (foodRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("已存在同名食物: " + request.getName());
        }
        // 校验分类存在，避免幽灵分类引用（保存后前端 join 分类时 500）
        if (!foodCategoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("分类不存在，请重新选择");
        }
        Food food = Food.builder()
                .name(request.getName())
                .category(FoodCategory.builder().id(request.getCategoryId()).build())
                .caloriesPer100g(request.getCaloriesPer100g())
                .proteinPer100g(request.getProteinPer100g() != null ? request.getProteinPer100g() : 0)
                .fatPer100g(request.getFatPer100g() != null ? request.getFatPer100g() : 0)
                .carbsPer100g(request.getCarbsPer100g() != null ? request.getCarbsPer100g() : 0)
                .isPublic(false)
                .isCommon(false)
                .creator(User.builder().id(userId).build())
                .build();
        return foodRepository.save(food);
    }

    // 删除自定义食物（只能删除自己创建的，同时清理该食物的收藏记录）
    @Transactional
    public void deleteCustomFood(Long foodId, Long userId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new BusinessException("食物不存在: " + foodId));
        if (Boolean.TRUE.equals(food.getIsPublic())) {
            throw new BusinessException("公共食物不可删除");
        }
        if (food.getCreator() == null || !food.getCreator().getId().equals(userId)) {
            throw new BusinessException("无权删除该食物");
        }
        // 被饮食记录引用时禁止删除（硬删会触发外键异常 500），提示用户先清理使用记录
        if (mealRecordRepository.existsByFoodId(foodId)) {
            throw new BusinessException("该食物已被饮食记录使用，无法删除。请先删除引用它的饮食记录");
        }
        userFavoriteRepository.deleteByFoodId(foodId);
        foodRepository.delete(food);
    }

}