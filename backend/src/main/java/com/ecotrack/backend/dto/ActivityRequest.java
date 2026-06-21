package com.ecotrack.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class ActivityRequest {

    private LocalDate date; // Optional, defaults to today in service if null

    @NotBlank(message = "Transport type is required")
    private String transportType; // CAR, BIKE, BUS, NONE

    @Min(value = 0, message = "Distance must be non-negative")
    private double transportDistance;

    @Min(value = 0, message = "Electricity units must be non-negative")
    private double electricityUnits;

    @NotBlank(message = "Food habit is required (VEG/NON_VEG)")
    private String foodHabit; // VEG, NON_VEG

    @Min(value = 0, message = "Plastic items count must be non-negative")
    private int plasticItems;
}
