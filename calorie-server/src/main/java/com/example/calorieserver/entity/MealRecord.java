package com.example.calorieserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Column(nullable = false)
    private Double grams;  // 吃了多少克

    @Column(nullable = false)
    private Double totalCalories;  // 实际摄入热量（冗余存储）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealType mealType;

    @Column(nullable = false)
    private LocalDate mealDate;  // 哪一天吃的

    @Column(length = 200)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }
}