package com.ecotrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private double todayCarbon;
    private int ecoScore;
    private int currentStreak;
    private int maxStreak;
    private List<String> badges;
    private List<WeeklyTrendItem> weeklyTrend;
    private CategoryBreakdown categoryBreakdown;
    private List<String> suggestions;
    private List<String> insights;
    private String dailyChallenge;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyTrendItem {
        private LocalDate date;
        private double carbon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryBreakdown {
        private double transport;
        private double food;
        private double electricity;
        private double plastic;
    }
}
