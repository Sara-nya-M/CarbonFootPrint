package com.ecotrack.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "badge_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_type", nullable = false)
    private String badgeType; // ECO_BEGINNER, GREEN_WARRIOR, SUSTAINABILITY_HERO

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    @PrePersist
    protected void onCreate() {
        awardedAt = LocalDateTime.now();
    }
}
