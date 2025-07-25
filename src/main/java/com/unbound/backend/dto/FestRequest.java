package com.unbound.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FestRequest {
    @NotBlank(message = "Fest name is required")
    private String fname;

    @Size(max = 1000, message = "Description too long")
    private String fdescription;

    @NotBlank(message = "Start date is required")
    private String startDate;

    @NotBlank(message = "End date is required")
    private String endDate;
} 