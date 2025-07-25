package com.unbound.backend.dto;

import lombok.Data;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class EventRegistrationRequest {
    @NotNull(message = "Event ID is required")
    private Integer eventId;
    @NotBlank(message = "Registration type is required")
    private String registrationType; // "solo" or "team"
    private Integer teamId; // for joining existing team
    private String teamName; // for creating new team
    private List<Integer> memberIds; // for team registration (including self)
} 