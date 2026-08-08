package com.example.calorieserver.service;

import com.example.calorieserver.dto.CreateFoodRequest;
import com.example.calorieserver.dto.FoodPageResponse;
import com.example.calorieserver.entity.Food;
import com.example.calorieserver.entity.FoodCategory;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.UserFavorite;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.FoodCategoryRepository;
import com.example.calorieserver.repository.FoodRepository;
import com.example.calorieserver.repository.MealRecordRepository;
import com.example.calorieserver.repository.UserFavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FoodService 单元测试：查询透传、搜索（空关键词不查库/trim）、分页映射、收藏增删查、
 * 自定义食物创建（重名/分类校验/默认值）与删除（公共不可删/归属/被引用分支）。
 */
@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private FoodCategoryRepository foodCategoryRepository;

    @Mock
    private UserFavoriteRepository userFavoriteRepository;

    @Mock
    private MealRecordRepository mealRecordRepository;

    @InjectMocks
    private FoodService foodService;

    private Food food(Long id, String name, Double calories) {
        return Food.builder().id(id).name(name).caloriesPer100g(calories)
                .isPublic(true).isCommon(true).build();
    }

    private FoodCategory category(Long id, String name) {
        return FoodCategory.builder().id(id).name(name).build();
    }

    private CreateFoodRequest createReq(String name, Long categoryId, Double calories) {
        CreateFoodRequest req = new CreateFoodRequest();
        req.setName(name);
        req.setCategoryId(categoryId);
        req.setCaloriesPer100g(calories);
        return req;
    }

    // ===== 分类 / 查询透传 =====

    @Test
    void getAllCategories_returnsAll() {
        when(foodCategoryRepository.findAll()).thenReturn(List.of(category(1L, "主食"), category(2L, "蔬果")));
        assertEquals(2, foodService.getAllCategories().size());
    }

    @Test
    void getCommonFoods_returnsList() {
        when(foodRepository.findByIsPublicTrueAndIsCommonTrue()).thenReturn(List.of(food(1L, "鸡蛋", 144.0)));
        assertEquals(1, foodService.getCommonFoods().size());
    }

    @Test
    void getFoodsByCategory_passthrough() {
        when(foodRepository.findByCategoryIdAndIsPublicTrueOrderByCaloriesPer100gAsc(2L))
                .thenReturn(List.of(food(1L, "苹果", 53.0)));
        assertEquals("苹果", foodService.getFoodsByCategory(2L).get(0).getName());
    }

    @Test
    void getCommonFoodsByCategory_passthrough() {
        when(foodRepository.findByCategoryIdAndIsPublicTrueAndIsCommonTrueOrderByCaloriesPer100gAsc(2L))
                .thenReturn(List.of(food(1L, "苹果", 53.0)));
        assertEquals(1, foodService.getCommonFoodsByCategory(2L).size());
    }

    @Test
    void getUserFoods_passthrough() {
        when(foodRepository.findByCreatorId(1L)).thenReturn(List.of(food(1L, "自制沙拉", 120.0)));
        assertEquals(1, foodService.getUserFoods(1L).size());
    }

    // ===== 搜索 =====

    @Test
    void searchFoods_blank_returnsEmpty_noRepoCall() {
        assertEquals(0, foodService.searchFoods(null).size());
        assertEquals(0, foodService.searchFoods("").size());
        assertEquals(0, foodService.searchFoods("   ").size());
        verify(foodRepository, never()).findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc(anyString());
    }

    @Test
    void searchFoods_trimsKeyword() {
        when(foodRepository.findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc("鸡蛋"))
                .thenReturn(List.of(food(1L, "鸡蛋", 144.0)));
        assertEquals(1, foodService.searchFoods(" 鸡蛋 ").size());
        verify(foodRepository).findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc("鸡蛋");
    }

    // ===== 分页 =====

    @Test
    void getFoodLibraryPage_mapsPage() {
        Page<Food> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of(food(1L, "米饭", 116.0)));
        when(page.getTotalElements()).thenReturn(42L);
        when(page.hasNext()).thenReturn(true);
        when(foodRepository.findByIsPublicTrueOrderByCaloriesPer100gAsc(any(Pageable.class))).thenReturn(page);

        FoodPageResponse resp = foodService.getFoodLibraryPage(0, 20);

        assertEquals(1, resp.getItems().size());
        assertEquals(42L, resp.getTotal());
        assertTrue(resp.isHasMore());
    }

    @Test
    void searchFoodsPage_mapsPage() {
        Page<Food> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of(food(1L, "鸡蛋", 144.0)));
        when(page.getTotalElements()).thenReturn(3L);
        when(page.hasNext()).thenReturn(false);
        when(foodRepository.findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc(eq("鸡蛋"), any(Pageable.class)))
                .thenReturn(page);

        FoodPageResponse resp = foodService.searchFoodsPage("鸡蛋", 0, 20);

        assertEquals(3L, resp.getTotal());
        assertFalse(resp.isHasMore());
    }

    // ===== 收藏 =====

    @Test
    void addFavorite_alreadyFavorited_throws() {
        when(userFavoriteRepository.findByUserIdAndFoodId(1L, 2L))
                .thenReturn(Optional.of(UserFavorite.builder().id(1L).build()));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodService.addFavorite(1L, 2L));
        assertEquals("已收藏该食物", ex.getMessage());
    }

    @Test
    void addFavorite_foodNotFound_throws() {
        when(userFavoriteRepository.findByUserIdAndFoodId(1L, 2L)).thenReturn(Optional.empty());
        when(foodRepository.findById(2L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> foodService.addFavorite(1L, 2L));
        assertEquals("食物不存在: 2", ex.getMessage());
    }

    @Test
    void addFavorite_success_savesFoodName() {
        when(userFavoriteRepository.findByUserIdAndFoodId(1L, 2L)).thenReturn(Optional.empty());
        when(foodRepository.findById(2L)).thenReturn(Optional.of(food(2L, "鸡蛋", 144.0)));
        when(userFavoriteRepository.save(any(UserFavorite.class))).thenAnswer(inv -> inv.getArgument(0));

        foodService.addFavorite(1L, 2L);

        ArgumentCaptor<UserFavorite> captor = ArgumentCaptor.forClass(UserFavorite.class);
        verify(userFavoriteRepository).save(captor.capture());
        assertEquals("鸡蛋", captor.getValue().getFoodName()); // 冗余存菜名
        assertEquals(1L, captor.getValue().getUser().getId());
        assertEquals(2L, captor.getValue().getFood().getId());
    }

    @Test
    void removeFavorite_callsRepo() {
        foodService.removeFavorite(1L, 2L);
        verify(userFavoriteRepository).deleteByUserIdAndFoodId(1L, 2L);
    }

    @Test
    void getFavoriteFoods_mapsFood() {
        Food egg = food(2L, "鸡蛋", 144.0);
        when(userFavoriteRepository.findByUserId(1L))
                .thenReturn(List.of(UserFavorite.builder().id(1L).user(User.builder().id(1L).build()).food(egg).build()));
        assertEquals("鸡蛋", foodService.getFavoriteFoods(1L).get(0).getName());
    }

    @Test
    void isFavorited_present() {
        when(userFavoriteRepository.findByUserIdAndFoodId(1L, 2L))
                .thenReturn(Optional.of(UserFavorite.builder().id(1L).build()));
        assertTrue(foodService.isFavorited(1L, 2L));
    }

    @Test
    void isFavorited_absent() {
        when(userFavoriteRepository.findByUserIdAndFoodId(1L, 2L)).thenReturn(Optional.empty());
        assertFalse(foodService.isFavorited(1L, 2L));
    }

    @Test
    void getFavoriteFoodIds_mapsIds() {
        when(userFavoriteRepository.findByUserId(1L)).thenReturn(List.of(
                UserFavorite.builder().id(1L).food(Food.builder().id(3L).build()).build(),
                UserFavorite.builder().id(2L).food(Food.builder().id(4L).build()).build()));
        assertEquals(List.of(3L, 4L), foodService.getFavoriteFoodIds(1L));
    }

    // ===== 自定义食物创建 =====

    @Test
    void createCustomFood_nameExists_throws() {
        when(foodRepository.findByName("可乐")).thenReturn(Optional.of(food(1L, "可乐", 43.0)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> foodService.createCustomFood(1L, createReq("可乐", 5L, 43.0)));
        assertEquals("已存在同名食物: 可乐", ex.getMessage());
    }

    @Test
    void createCustomFood_categoryMissing_throws() {
        when(foodRepository.findByName("自制沙拉")).thenReturn(Optional.empty());
        when(foodCategoryRepository.existsById(9L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> foodService.createCustomFood(1L, createReq("自制沙拉", 9L, 120.0)));
        assertEquals("分类不存在，请重新选择", ex.getMessage());
    }

    @Test
    void createCustomFood_success_setsDefaults() {
        when(foodRepository.findByName("自制沙拉")).thenReturn(Optional.empty());
        when(foodCategoryRepository.existsById(9L)).thenReturn(true);
        when(foodRepository.save(any(Food.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateFoodRequest req = createReq("自制沙拉", 9L, 120.0); // 营养字段不传
        Food saved = foodService.createCustomFood(1L, req);

        assertEquals("自制沙拉", saved.getName());
        assertEquals(9L, saved.getCategory().getId());
        assertEquals(120.0, saved.getCaloriesPer100g(), 0.001);
        assertEquals(0.0, saved.getProteinPer100g(), 0.001); // null → 0
        assertEquals(0.0, saved.getFatPer100g(), 0.001);
        assertEquals(0.0, saved.getCarbsPer100g(), 0.001);
        assertFalse(saved.getIsPublic());
        assertFalse(saved.getIsCommon());
        assertEquals(1L, saved.getCreator().getId());
    }

    // ===== 自定义食物删除 =====

    @Test
    void deleteCustomFood_notFound_throws() {
        when(foodRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> foodService.deleteCustomFood(5L, 1L));
    }

    @Test
    void deleteCustomFood_isPublic_throws() {
        Food publicFood = food(5L, "米饭", 116.0); // isPublic=true
        when(foodRepository.findById(5L)).thenReturn(Optional.of(publicFood));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodService.deleteCustomFood(5L, 1L));
        assertEquals("公共食物不可删除", ex.getMessage());
    }

    @Test
    void deleteCustomFood_noCreator_throws() {
        Food custom = Food.builder().id(5L).name("自制").caloriesPer100g(100.0)
                .isPublic(false).isCommon(false).build(); // creator = null
        when(foodRepository.findById(5L)).thenReturn(Optional.of(custom));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodService.deleteCustomFood(5L, 1L));
        assertEquals("无权删除该食物", ex.getMessage());
    }

    @Test
    void deleteCustomFood_otherCreator_throws() {
        Food custom = Food.builder().id(5L).name("自制").caloriesPer100g(100.0)
                .isPublic(false).isCommon(false).creator(User.builder().id(2L).build()).build();
        when(foodRepository.findById(5L)).thenReturn(Optional.of(custom));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodService.deleteCustomFood(5L, 1L));
        assertEquals("无权删除该食物", ex.getMessage());
    }

    @Test
    void deleteCustomFood_referencedByMealRecord_throws() {
        Food custom = Food.builder().id(5L).name("自制").caloriesPer100g(100.0)
                .isPublic(false).isCommon(false).creator(User.builder().id(1L).build()).build();
        when(foodRepository.findById(5L)).thenReturn(Optional.of(custom));
        when(mealRecordRepository.existsByFoodId(5L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> foodService.deleteCustomFood(5L, 1L));
        assertEquals("该食物已被饮食记录使用，无法删除。请先删除引用它的饮食记录", ex.getMessage());
    }

    @Test
    void deleteCustomFood_success_deletesFavoriteAndFood() {
        Food custom = Food.builder().id(5L).name("自制").caloriesPer100g(100.0)
                .isPublic(false).isCommon(false).creator(User.builder().id(1L).build()).build();
        when(foodRepository.findById(5L)).thenReturn(Optional.of(custom));
        when(mealRecordRepository.existsByFoodId(5L)).thenReturn(false);

        foodService.deleteCustomFood(5L, 1L);

        verify(userFavoriteRepository).deleteByFoodId(5L);
        verify(foodRepository).delete(custom);
    }
}
