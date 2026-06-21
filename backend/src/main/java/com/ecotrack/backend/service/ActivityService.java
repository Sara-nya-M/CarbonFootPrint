package com.ecotrack.backend.service;

import com.ecotrack.backend.dto.ActivityRequest;
import com.ecotrack.backend.model.Badge;
import com.ecotrack.backend.model.DailyActivity;
import com.ecotrack.backend.model.Streak;
import com.ecotrack.backend.model.User;
import com.ecotrack.backend.repository.BadgeRepository;
import com.ecotrack.backend.repository.DailyActivityRepository;
import com.ecotrack.backend.repository.StreakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

    @Autowired
    private DailyActivityRepository activityRepository;

    @Autowired
    private StreakRepository streakRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Transactional
    public DailyActivity saveActivity(User user, ActivityRequest request) {
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();

        // 1. Calculate Carbon Category emissions
        double transportFactor = 0.0;
        String transportType = request.getTransportType().toUpperCase();
        if ("CAR".equals(transportType)) {
            transportFactor = 0.21;
        } else if ("BIKE".equals(transportType)) {
            transportFactor = 0.1;
        } else if ("BUS".equals(transportType)) {
            transportFactor = 0.05;
        }

        double transportCarbon = request.getTransportDistance() * transportFactor;
        double electricityCarbon = request.getElectricityUnits() * 0.5;
        double foodCarbon = "NON_VEG".equalsIgnoreCase(request.getFoodHabit()) ? 2.5 : 1.0;
        double plasticCarbon = request.getPlasticItems() * 0.05;
        double totalCarbon = transportCarbon + electricityCarbon + foodCarbon + plasticCarbon;

        // 2. Check if record exists for this date and user
        Optional<DailyActivity> existingActivity = activityRepository.findByUserAndDate(user, date);
        DailyActivity activity;

        if (existingActivity.isPresent()) {
            activity = existingActivity.get();
            activity.setTransportType(request.getTransportType());
            activity.setTransportDistance(request.getTransportDistance());
            activity.setElectricityUnits(request.getElectricityUnits());
            activity.setFoodHabit(request.getFoodHabit());
            activity.setPlasticItems(request.getPlasticItems());
            activity.setTransportCarbon(transportCarbon);
            activity.setElectricityCarbon(electricityCarbon);
            activity.setFoodCarbon(foodCarbon);
            activity.setPlasticCarbon(plasticCarbon);
            activity.setCarbonFootprintTotal(totalCarbon);
        } else {
            activity = DailyActivity.builder()
                    .user(user)
                    .date(date)
                    .transportType(request.getTransportType())
                    .transportDistance(request.getTransportDistance())
                    .electricityUnits(request.getElectricityUnits())
                    .foodHabit(request.getFoodHabit())
                    .plasticItems(request.getPlasticItems())
                    .transportCarbon(transportCarbon)
                    .electricityCarbon(electricityCarbon)
                    .foodCarbon(foodCarbon)
                    .plasticCarbon(plasticCarbon)
                    .carbonFootprintTotal(totalCarbon)
                    .build();
        }

        DailyActivity savedActivity = activityRepository.save(activity);

        // 3. Update User Streaks
        updateStreak(user, date);

        // 4. Award Badges
        checkAndAwardBadges(user);

        return savedActivity;
    }

    private void updateStreak(User user, LocalDate date) {
        Streak streak = streakRepository.findByUser(user)
                .orElseGet(() -> Streak.builder()
                        .user(user)
                        .currentStreak(0)
                        .maxStreak(0)
                        .lastActivityDate(null)
                        .build());

        if (streak.getLastActivityDate() == null) {
            streak.setCurrentStreak(1);
            streak.setMaxStreak(Math.max(1, streak.getMaxStreak()));
            streak.setLastActivityDate(date);
        } else if (date.isAfter(streak.getLastActivityDate())) {
            if (streak.getLastActivityDate().plusDays(1).equals(date)) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1);
            }
            streak.setMaxStreak(Math.max(streak.getCurrentStreak(), streak.getMaxStreak()));
            streak.setLastActivityDate(date);
        } else if (date.equals(streak.getLastActivityDate())) {
            // Same day update, streak remains unchanged
        } else {
            // Log entry for a past day, do not change current streak active count
        }

        streakRepository.save(streak);
    }

    private void checkAndAwardBadges(User user) {
        // 1. Eco Beginner
        if (!badgeRepository.existsByUserAndBadgeType(user, "ECO_BEGINNER")) {
            Badge badge = Badge.builder()
                    .user(user)
                    .badgeType("ECO_BEGINNER")
                    .awardedAt(LocalDateTime.now())
                    .build();
            badgeRepository.save(badge);
        }

        // Fetch Streak details
        Streak streak = streakRepository.findByUser(user).orElse(null);
        if (streak == null) return;

        // 2. Green Warrior (streak >= 3)
        if (streak.getCurrentStreak() >= 3 && !badgeRepository.existsByUserAndBadgeType(user, "GREEN_WARRIOR")) {
            Badge badge = Badge.builder()
                    .user(user)
                    .badgeType("GREEN_WARRIOR")
                    .awardedAt(LocalDateTime.now())
                    .build();
            badgeRepository.save(badge);
        }

        // 3. Sustainability Hero (streak >= 7 AND average carbon footprint in past 7 days is < 5 kg CO2)
        if (streak.getCurrentStreak() >= 7 && !badgeRepository.existsByUserAndBadgeType(user, "SUSTAINABILITY_HERO")) {
            // Fetch past 7 days logs
            List<DailyActivity> last7DaysActivities = activityRepository.findByUserAndDateBetweenOrderByDateAsc(
                    user, LocalDate.now().minusDays(6), LocalDate.now()
            );
            if (last7DaysActivities.size() >= 7) {
                double total = last7DaysActivities.stream()
                        .mapToDouble(DailyActivity::getCarbonFootprintTotal)
                        .sum();
                double average = total / last7DaysActivities.size();
                if (average < 5.0) {
                    Badge badge = Badge.builder()
                            .user(user)
                            .badgeType("SUSTAINABILITY_HERO")
                            .awardedAt(LocalDateTime.now())
                            .build();
                    badgeRepository.save(badge);
                }
            }
        }
    }
}
