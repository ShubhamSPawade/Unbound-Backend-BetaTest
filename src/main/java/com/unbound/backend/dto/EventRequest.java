package com.unbound.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EventRequest {
    @NotBlank(message = "Event name is required")
    private String ename;

    @Size(max = 1000, message = "Description too long")
    private String edescription;

    @NotBlank(message = "Event date is required")
    private String eventDate;

    @Min(value = 0, message = "Fees cannot be negative")
    private Integer fees = 0;

    private String location;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "teamIsAllowed is required")
    private Boolean teamIsAllowed = false;

    private Integer festId; // Optional, for linking to a fest

    private String category; // e.g., Technical, Cultural, Sports, etc.
    private String mode; // Online, Offline
    private String posterUrl; // URL or path to event poster/banner
} 