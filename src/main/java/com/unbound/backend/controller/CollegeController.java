package com.unbound.backend.controller;

import com.unbound.backend.entity.College;
import com.unbound.backend.entity.User;
import com.unbound.backend.repository.CollegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/college")
public class CollegeController {
    @Autowired
    private CollegeRepository collegeRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body("Forbidden: Only colleges can access this endpoint");
        }
        College college = collegeRepository.findAll().stream()
                .filter(c -> c.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
        if (college == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(college);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User user, @RequestBody College updated) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body("Forbidden: Only colleges can access this endpoint");
        }
        College college = collegeRepository.findAll().stream()
                .filter(c -> c.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
        if (college == null) {
            return ResponseEntity.notFound().build();
        }
        college.setCname(updated.getCname());
        college.setCdescription(updated.getCdescription());
        college.setAddress(updated.getAddress());
        college.setContactEmail(updated.getContactEmail());
        collegeRepository.save(college);
        return ResponseEntity.ok(college);
    }
} 