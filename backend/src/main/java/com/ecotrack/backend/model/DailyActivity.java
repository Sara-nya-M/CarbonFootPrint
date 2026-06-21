package com.ecotrack.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_activities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "transport_type", nullable = false)
    private String transportType; // CAR, BIKE, BUS, NONE

    @Column(name = "transport_distance", nullable = false)
    private double transportDistance;

    @Column(name = "electricity_units", nullable = false)
    private double electricityUnits;

    @Column(name = "food_habit", nullable = false)
    private String foodHabit; // VEG, NON_VEG

    @Column(name = "plastic_items", nullable = false)
    private int plasticItems;

    // Calculated Carbon Footprints (in kg CO2)
    @Column(name = "transport_carbon", nullable = false)
    private double transportCarbon;

    @Column(name = "electricity_carbon", nullable = false)
    private double electricityCarbon;

    @Column(name = "food_carbon", nullable = false)
    private double foodCarbon;

    @Column(name = "plastic_carbon", nullable = false)
    private double plasticCarbon;

    @Column(name = "carbon_footprint_total", nullable = false)
    private double carbonFootprintTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
