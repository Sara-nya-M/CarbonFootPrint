package com.ecotrack.backend.repository;

import com.ecotrack.backend.model.DailyActivity;
import com.ecotrack.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyActivityRepository extends JpaRepository<DailyActivity, UUID> {
    Optional<DailyActivity> findByUserAndDate(User user, LocalDate date);
    List<DailyActivity> findByUserAndDateBetweenOrderByDateAsc(User user, LocalDate startDate, LocalDate endDate);
    List<DailyActivity> findByUserOrderByDateDesc(User user);
}
