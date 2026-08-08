package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.dto.CreateFoodRequest;
import com.example.calorieserver.dto.FoodPageResponse;
import com.example.calorieserver.entity.Food;
import com.example.calorieserver.entity.FoodCategory;
import com.example.calorieserver.service.FoodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FoodController 切片测试：分类/常见/搜索/收藏/自定义 + @Valid、分页钳位。
 * 业务逻辑在 FoodService（已有 FoodServiceTest 覆盖），此处只测 controller 切片。
 */
@ExtendWith(MockitoExtension.class)
class FoodControllerTest {

    @Mock
    private FoodService foodService;

    @InjectMocks
    private FoodController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Food food(long id, String name) {
        return Food.builder().id(id).name(name).build();
    }

    // ===== 分类 / 常见 / 查询 =====

    @Test
    void getAllCategories_returnsList() throws Exception {
        when(foodService.getAllCategories())
                .thenReturn(List.of(FoodCategory.builder().id(1L).name("主食").build()));
        mockMvc.perform(get("/api/foods/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("主食"));
        verify(foodService).getAllCategories();
    }

    @Test
    void getCommonFoods_returnsList() throws Exception {
        when(foodService.getCommonFoods()).thenReturn(List.of(food(1L, "鸡胸肉")));
        mockMvc.perform(get("/api/foods/common"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("鸡胸肉"));
        verify(foodService).getCommonFoods();
    }

    @Test
    void getByCategory_delegates() throws Exception {
        when(foodService.getFoodsByCategory(3L)).thenReturn(List.of());
        mockMvc.perform(get("/api/foods/category/3"))
                .andExpect(status().isOk());
        verify(foodService).getFoodsByCategory(3L);
    }

    @Test
    void getCommonByCategory_delegates() throws Exception {
        when(foodService.getCommonFoodsByCategory(3L)).thenReturn(List.of());
        mockMvc.perform(get("/api/foods/category/3/common"))
                .andExpect(status().isOk());
        verify(foodService).getCommonFoodsByCategory(3L);
    }

    @Test
    void search_delegates() throws Exception {
        when(foodService.searchFoods("鸡")).thenReturn(List.of(food(1L, "鸡肉")));
        mockMvc.perform(get("/api/foods/search").param("keyword", "鸡"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("鸡肉"));
        verify(foodService).searchFoods("鸡");
    }

    // ===== 分页 =====

    @Test
    void getFoodLibrary_delegatesWithParams() throws Exception {
        FoodPageResponse page = new FoodPageResponse(List.of(food(1L, "苹果")), 1L, false);
        when(foodService.getFoodLibraryPage(0, 50)).thenReturn(page);
        mockMvc.perform(get("/api/foods/library"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("苹果"));
        verify(foodService).getFoodLibraryPage(0, 50);
    }

    @Test
    void getFoodLibrary_clampsSize() throws Exception {
        when(foodService.getFoodLibraryPage(0, 200)).thenReturn(new FoodPageResponse(List.of(), 0, false));
        mockMvc.perform(get("/api/foods/library").param("size", "9999"))
                .andExpect(status().isOk());
        verify(foodService).getFoodLibraryPage(0, 200);
    }

    @Test
    void searchFoodsPage_delegates() throws Exception {
        when(foodService.searchFoodsPage("米", 0, 50)).thenReturn(new FoodPageResponse(List.of(), 0, false));
        mockMvc.perform(get("/api/foods/search/page").param("keyword", "米"))
                .andExpect(status().isOk());
        verify(foodService).searchFoodsPage("米", 0, 50);
    }

    // ===== 收藏 =====

    @Test
    void addFavorite_returns204AndDelegates() throws Exception {
        mockMvc.perform(post("/api/foods/favorite").param("userId", "1").param("foodId", "2"))
                .andExpect(status().isNoContent());
        verify(foodService).addFavorite(1L, 2L);
    }

    @Test
    void removeFavorite_delegates() throws Exception {
        mockMvc.perform(delete("/api/foods/favorite").param("userId", "1").param("foodId", "2"))
                .andExpect(status().isNoContent());
        verify(foodService).removeFavorite(1L, 2L);
    }

    @Test
    void getFavoriteFoodIds_returnsList() throws Exception {
        when(foodService.getFavoriteFoodIds(1L)).thenReturn(List.of(1L, 2L));
        mockMvc.perform(get("/api/foods/favorites/ids/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(2));
        verify(foodService).getFavoriteFoodIds(1L);
    }

    // ===== 自定义食物 =====

    @Test
    void createCustomFood_success_delegates() throws Exception {
        when(foodService.createCustomFood(eq(1L), any(CreateFoodRequest.class))).thenReturn(food(9L, "自创"));
        mockMvc.perform(post("/api/foods/custom").param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"自创\",\"categoryId\":1,\"caloriesPer100g\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
        verify(foodService).createCustomFood(eq(1L), any(CreateFoodRequest.class));
    }

    @Test
    void createCustomFood_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/foods/custom").param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"caloriesPer100g\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("食物名称不能为空"));
        verifyNoInteractions(foodService);
    }

    @Test
    void createCustomFood_missingCategory_returns400() throws Exception {
        mockMvc.perform(post("/api/foods/custom").param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"自创\",\"caloriesPer100g\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择分类"));
    }

    @Test
    void deleteCustomFood_delegates() throws Exception {
        mockMvc.perform(delete("/api/foods/custom/5").param("userId", "1"))
                .andExpect(status().isNoContent());
        verify(foodService).deleteCustomFood(5L, 1L);
    }

    @Test
    void getUserFoods_delegates() throws Exception {
        when(foodService.getUserFoods(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/foods/creator/1"))
                .andExpect(status().isOk());
        verify(foodService).getUserFoods(1L);
    }
}