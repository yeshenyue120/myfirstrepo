package com.example.calorieserver.repository;

import com.example.calorieserver.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    // 查某用户的所有收藏
    List<UserFavorite> findByUserId(Long userId);

    // 查某用户是否已收藏某食物
    Optional<UserFavorite> findByUserIdAndFoodId(Long userId, Long foodId);

    // 取消收藏
    void deleteByUserIdAndFoodId(Long userId, Long foodId);

    // 删除某食物的所有收藏记录（删除自定义食物时清理外键）
    void deleteByFoodId(Long foodId);

    // 某用户收藏数
    long countByUserId(Long userId);
}