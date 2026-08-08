package com.example.calorieserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // 主食、肉类、蔬菜、水果、零食、饮品...

    @Column(length = 100)
    private String icon;  // 图标名称或URL，暂时可空

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}