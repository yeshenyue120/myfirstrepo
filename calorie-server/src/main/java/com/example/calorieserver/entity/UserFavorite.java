package com.example.calorieserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "food_id"})  // 同一个用户不能重复收藏同一个食物
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    // 冗余存储收藏时的菜名，方便直接查表（不 join foods 表也能看到名称）
    @Column(name = "food_name", length = 100)
    private String foodName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}