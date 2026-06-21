package com.ecotrack.backend.repository;

import com.ecotrack.backend.model.Streak;
import com.ecotrack.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreakRepository extends JpaRepository<Streak, UUID> {
    Optional<Streak> findByUser(User user);

    @Query("SELECT s FROM Streak s JOIN FETCH s.user ORDER BY s.currentStreak DESC")
    List<Streak> findAllOrderByCurrentStreakDesc();
}
