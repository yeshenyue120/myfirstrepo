package com.example.calorieserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private Double weightKg;

    @Column(nullable = false)
    private LocalDate recordedDate;

    // 体成分（可选，随体重记录追踪趋势）
    @Column
    private Double bodyFatPct;

    @Column
    private Double waistCm;

    @Column
    private Double hipCm;

    @Column(length = 200)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}