package com.example.calorieserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "foods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100,unique = true)
    private String name;

    // 多对一：多个食物属于一个分类
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FoodCategory category;

    // ===== 每100克的营养成分 =====
    @Column(nullable = false)
    private Double caloriesPer100g;

    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbsPer100g;

    // 是否属于公共食物库
    @Column(nullable = false)
    private Boolean isPublic;

    // 是否常见食物（在公共食物库中高频展示，如鸡蛋、米饭等）
    @Column(nullable = false)
    private Boolean isCommon = false;

    // 如果是用户自定义的，记录创建者（公共食物这里为 null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @JsonIgnore
    private User creator;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}