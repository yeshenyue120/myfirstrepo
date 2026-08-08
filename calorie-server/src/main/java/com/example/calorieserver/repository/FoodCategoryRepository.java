package com.example.calorieserver.repository;

import com.example.calorieserver.entity.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    // 根据分类名查找
    Optional<FoodCategory> findByName(String name);

    // 检查分类名是否存在
    boolean existsByName(String name);
}