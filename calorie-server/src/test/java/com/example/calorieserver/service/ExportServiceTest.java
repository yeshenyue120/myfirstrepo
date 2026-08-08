package com.example.calorieserver.service;

import com.example.calorieserver.dto.MealRecordResponse;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.entity.Food;
import com.example.calorieserver.entity.MealRecord;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExportService 单元测试：用户不存在抛异常、成功导出全部分区（8 段数据 + mealRecords 响应转换）。
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeightRecordRepository weightRecordRepository;

    @Mock
    private MealRecordRepository mealRecordRepository;

    @Mock
    private ExerciseRecordRepository exerciseRecordRepository;

    @Mock
    private WaterRecordRepository waterRecordRepository;

    @Mock
    private SleepRecordRepository sleepRecordRepository;

    @Mock
    private UserFavoriteRepository userFavoriteRepository;

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private ExportService exportService;

    private User user() {
        return User.builder().id(1L).username("tester").email("tester@example.com")
                .role(User.Role.USER).createdAt(LocalDateTime.of(2026, 8, 1, 12, 0)).build();
    }

    private Food food() {
        return Food.builder().id(2L).name("米饭").caloriesPer100g(116.0)
                .isPublic(true).isCommon(true).build();
    }

    private MealRecord mealRecord() {
        return MealRecord.builder().id(1L).user(User.builder().id(1L).build()).food(food())
                .grams(100.0).totalCalories(116.0).mealType(MealRecord.MealType.LUNCH)
                .mealDate(LocalDate.of(2026, 8, 1)).build();
    }

    @Test
    void exportUserData_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> exportService.exportUserData(1L));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void exportUserData_success_returnsAllSections() {
        WeightRecord weight = WeightRecord.builder().id(1L).user(User.builder().id(1L).build())
                .weightKg(70.0).recordedDate(LocalDate.of(2026, 8, 1)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(weightRecordRepository.findByUserIdOrderByRecordedDateDescIdDesc(1L)).thenReturn(List.of(weight));
        when(mealRecordRepository.findByUserIdAndMealDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(mealRecord()));
        when(exerciseRecordRepository.findByUserIdAndRecordDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(waterRecordRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(sleepRecordRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(userFavoriteRepository.findByUserId(1L)).thenReturn(List.of());
        when(foodRepository.findByCreatorId(1L)).thenReturn(List.of(food()));

        Map<String, Object> result = exportService.exportUserData(1L);

        assertEquals(8, result.size());
        assertTrue(result.containsKey("user"));
        assertTrue(result.containsKey("weightRecords"));
        assertTrue(result.containsKey("mealRecords"));
        assertTrue(result.containsKey("exerciseRecords"));
        assertTrue(result.containsKey("waterRecords"));
        assertTrue(result.containsKey("sleepRecords"));
        assertTrue(result.containsKey("favorites"));
        assertTrue(result.containsKey("customFoods"));

        UserResponse userResp = (UserResponse) result.get("user");
        assertEquals("tester", userResp.getUsername());

        assertEquals(1, ((List<?>) result.get("weightRecords")).size());
        assertEquals(1, ((List<?>) result.get("customFoods")).size());

        // mealRecords 已转响应对象（foodName 来自 food，不再引用 Food 实体）
        @SuppressWarnings("unchecked")
        List<MealRecordResponse> mealResp = (List<MealRecordResponse>) result.get("mealRecords");
        assertEquals(1, mealResp.size());
        assertEquals("米饭", mealResp.get(0).getFoodName());
        assertEquals(116.0, mealResp.get(0).getTotalCalories(), 0.001);

        verify(weightRecordRepository).findByUserIdOrderByRecordedDateDescIdDesc(1L);
        verify(mealRecordRepository).findByUserIdAndMealDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(exerciseRecordRepository).findByUserIdAndRecordDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(waterRecordRepository).findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(sleepRecordRepository).findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class));
        verify(userFavoriteRepository).findByUserId(1L);
        verify(foodRepository).findByCreatorId(1L);
    }
}
