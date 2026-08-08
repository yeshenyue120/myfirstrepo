package com.example.calorieserver.repository;

import com.example.calorieserver.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    // 根据名称查食物（名称唯一）
    Optional<Food> findByName(String name);

    // 公共库关键词搜索（按卡路里升序排列）
    List<Food> findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc(String keyword);

    // 查公共食物库
    List<Food> findByIsPublicTrue();

    // 查某用户自定义的食物
    List<Food> findByCreatorId(Long creatorId);

    // 按分类 + 热量从低到高
    List<Food> findByCategoryIdAndIsPublicTrueOrderByCaloriesPer100gAsc(Long categoryId);

    // 查所有常见公共食物
    List<Food> findByIsPublicTrueAndIsCommonTrue();

    // 按分类查常见公共食物（分类页只展示常见）
    List<Food> findByCategoryIdAndIsPublicTrueAndIsCommonTrueOrderByCaloriesPer100gAsc(Long categoryId);

    // ===== 分页（食物库全量浏览） =====

    // 全库公共食物分页（按卡路里升序）
    Page<Food> findByIsPublicTrueOrderByCaloriesPer100gAsc(Pageable pageable);

    // 全库关键词搜索分页（按卡路里升序）
    Page<Food> findByNameContainingAndIsPublicTrueOrderByCaloriesPer100gAsc(String keyword, Pageable pageable);

}