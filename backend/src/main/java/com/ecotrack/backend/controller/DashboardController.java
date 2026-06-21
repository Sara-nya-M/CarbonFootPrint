package com.ecotrack.backend.controller;

import com.ecotrack.backend.dto.DashboardResponse;
import com.ecotrack.backend.model.*;
import com.ecotrack.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyActivityRepository activityRepository;

    @Autowired
    private StreakRepository streakRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        User user = getAuthenticatedUser();
        LocalDate today = LocalDate.now();

        // 1. Today's Carbon footprint
        Optional<DailyActivity> todayActivityOpt = activityRepository.findByUserAndDate(user, today);
        double todayCarbon = todayActivityOpt.map(DailyActivity::getCarbonFootprintTotal).orElse(0.0);

        // 2. Fetch past 7 days logs (including today)
        LocalDate startDate = today.minusDays(6);
        List<DailyActivity> last7Days = activityRepository.findByUserAndDateBetweenOrderByDateAsc(user, startDate, today);

        // 3. Calculate Eco Score
        int ecoScore = 0;
        if (!last7Days.isEmpty()) {
            double totalCarbonLast7Days = last7Days.stream()
                    .mapToDouble(DailyActivity::getCarbonFootprintTotal)
                    .sum();
            double averageCarbon = totalCarbonLast7Days / last7Days.size();
            // Score formula: 100 - (averageCarbon * 6)
            // Baseline carbon footprint: lower carbon -> higher score
            ecoScore = (int) Math.max(0, Math.min(100, 100 - (averageCarbon * 6)));
        }

        // 4. Fetch Streak details
        Optional<Streak> streakOpt = streakRepository.findByUser(user);
        int currentStreak = streakOpt.map(Streak::getCurrentStreak).orElse(0);
        int maxStreak = streakOpt.map(Streak::getMaxStreak).orElse(0);

        // 5. Fetch Badges
        List<Badge> badgeList = badgeRepository.findByUser(user);
        List<String> badges = badgeList.stream()
                .map(Badge::getBadgeType)
                .collect(Collectors.toList());

        // 6. Generate Weekly Trend (Last 7 Days)
        Map<LocalDate, Double> activityMap = last7Days.stream()
                .collect(Collectors.toMap(DailyActivity::getDate, DailyActivity::getCarbonFootprintTotal, (a, b) -> a));

        List<DashboardResponse.WeeklyTrendItem> weeklyTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            double carbon = activityMap.getOrDefault(date, 0.0);
            weeklyTrend.add(new DashboardResponse.WeeklyTrendItem(date, carbon));
        }

        // 7. Category Breakdown for Today
        DashboardResponse.CategoryBreakdown categoryBreakdown = DashboardResponse.CategoryBreakdown.builder()
                .transport(todayActivityOpt.map(DailyActivity::getTransportCarbon).orElse(0.0))
                .food(todayActivityOpt.map(DailyActivity::getFoodCarbon).orElse(0.0))
                .electricity(todayActivityOpt.map(DailyActivity::getElectricityCarbon).orElse(0.0))
                .plastic(todayActivityOpt.map(DailyActivity::getPlasticCarbon).orElse(0.0))
                .build();

        // 8. Generate Suggestions based on emissions
        List<String> suggestions = new ArrayList<>();
        if (todayActivityOpt.isPresent()) {
            DailyActivity act = todayActivityOpt.get();
            if (act.getTransportCarbon() > 2.0) {
                suggestions.add("Your transport carbon footprint is high. Consider using public transit, walking, or cycling.");
            }
            if ("NON_VEG".equalsIgnoreCase(act.getFoodHabit())) {
                suggestions.add("Incorporating vegetarian meals into your diet can significantly lower your carbon footprint.");
            }
            if (act.getElectricityUnits() > 5.0) {
                suggestions.add("Reduce electricity usage by turning off appliances at the wall and upgrading to LED bulbs.");
            }
            if (act.getPlasticItems() > 2) {
                suggestions.add("Avoid single-use plastics today. Try carrying a reusable water bottle or cloth bag.");
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Track your activity every day to gain personalized suggestions for carbon reduction.");
            suggestions.add("Unplug unused chargers and electronics to save phantom energy load.");
        }

        // 9. Generate Insights
        List<String> insights = new ArrayList<>();
        if (currentStreak > 0) {
            insights.add("Awesome! You are on a " + currentStreak + "-day carbon tracking streak.");
        } else {
            insights.add("Log your activity today to start your green streak!");
        }
        if (!badges.isEmpty()) {
            insights.add("You've unlocked " + badges.size() + " eco-badges! Review them on your profile.");
        } else {
            insights.add("Complete daily tasks and maintain streaks to unlock special Eco Badges.");
        }
        if (!last7Days.isEmpty()) {
            double totalCarbon = last7Days.stream().mapToDouble(DailyActivity::getCarbonFootprintTotal).sum();
            insights.add(String.format("You emitted %.2f kg of CO2 in total over the last 7 days.", totalCarbon));
        }

        // 10. Select Daily Challenge based on weekday
        String dailyChallenge;
        switch (today.getDayOfWeek()) {
            case MONDAY:
                dailyChallenge = "Meatless Monday: Eat entirely plant-based meals today!";
                break;
            case TUESDAY:
                dailyChallenge = "Power Saver: Switch off all unused lights and unplug chargers.";
                break;
            case WEDNESDAY:
                dailyChallenge = "Zero Waste Wednesday: Do not use any single-use plastic cups, bottles, or bags today.";
                break;
            case THURSDAY:
                dailyChallenge = "Eco Commute: Ride a bike, walk, or carpool for your commute.";
                break;
            case FRIDAY:
                dailyChallenge = "Local Friday: Purchase locally produced food or shop from local businesses.";
                break;
            case SATURDAY:
                dailyChallenge = "Digital Detox: Spend at least 3 hours offline to save electronic device power.";
                break;
            case SUNDAY:
            default:
                dailyChallenge = "Nature Sunday: Spend 30 minutes in a park or water local plants.";
                break;
        }

        DashboardResponse response = DashboardResponse.builder()
                .todayCarbon(todayCarbon)
                .ecoScore(ecoScore)
                .currentStreak(currentStreak)
                .maxStreak(maxStreak)
                .badges(badges)
                .weeklyTrend(weeklyTrend)
                .categoryBreakdown(categoryBreakdown)
                .suggestions(suggestions)
                .insights(insights)
                .dailyChallenge(dailyChallenge)
                .build();

        return ResponseEntity.ok(response);
    }
}
