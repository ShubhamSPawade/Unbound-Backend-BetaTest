package com.unbound.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
    @NotBlank(message = "Role is required")
    private String role; // "Student" or "College"
    // Student fields
    private String sname;
    private Integer collegeId; // NEW: for assigning college to student
    // College fields
    private String cname;
    private String cdescription;
    private String address;
    private String contactEmail;
} 