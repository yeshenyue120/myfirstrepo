package com.example.calorieserver.dto;

import java.time.LocalDate;

public record WeightHistoryPoint(LocalDate date, Double weightKg, Double bodyFatPct, Double waistCm, Double hipCm) {}
