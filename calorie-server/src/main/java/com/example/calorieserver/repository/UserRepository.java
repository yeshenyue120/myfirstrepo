package com.example.calorieserver.repository;

import com.example.calorieserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository  // 标记这是一个数据访问组件
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository<User, Long>：操作的是 User 表，主键类型是 Long

    // 根据邮箱查用户（登录用）
    Optional<User> findByEmail(String email);

    // 根据用户名查用户
    Optional<User> findByUsername(String username);

    // 检查邮箱是否已被注册
    boolean existsByEmail(String email);

    // 检查用户名是否已被占用
    boolean existsByUsername(String username);

    // ===== 新增 =====
    // 按角色查用户（管理员用）
    List<User> findByRole(User.Role role);
}