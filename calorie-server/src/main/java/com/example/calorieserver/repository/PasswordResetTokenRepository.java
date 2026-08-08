package com.example.calorieserver.repository;

import com.example.calorieserver.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // 生成新 token 前清理该用户旧 token，保证同一时刻只有一个有效重置链接
    void deleteByUserId(Long userId);
}
