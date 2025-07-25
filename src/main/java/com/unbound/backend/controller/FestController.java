package com.unbound.backend.controller;

import com.unbound.backend.dto.FestRequest;
import com.unbound.backend.entity.Fest;
import com.unbound.backend.entity.College;
import com.unbound.backend.entity.User;
import com.unbound.backend.repository.FestRepository;
import com.unbound.backend.repository.CollegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fests")
public class FestController {
    @Autowired
    private FestRepository festRepository;
    @Autowired
    private CollegeRepository collegeRepository;

    private College getCollegeForUser(User user) {
        return collegeRepository.findAll().stream()
                .filter(c -> c.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> listFests(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        List<Fest> fests = festRepository.findByCollege(college);
        return ResponseEntity.ok(fests);
    }

    @PostMapping
    public ResponseEntity<?> createFest(@AuthenticationPrincipal User user, @Valid @RequestBody FestRequest festRequest) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        // Duplicate name check
        boolean exists = festRepository.findByCollege(college).stream()
                .anyMatch(f -> f.getFname().equalsIgnoreCase(festRequest.getFname()));
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fest name already exists for this college"));
        }
        // Date validation
        if (!isValidDateRange(festRequest.getStartDate(), festRequest.getEndDate())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start date must be before end date"));
        }
        Fest fest = Fest.builder()
                .college(college)
                .fname(festRequest.getFname())
                .fdescription(festRequest.getFdescription())
                .startDate(festRequest.getStartDate())
                .endDate(festRequest.getEndDate())
                .build();
        Fest saved = festRepository.save(fest);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{fid}")
    public ResponseEntity<?> updateFest(@AuthenticationPrincipal User user, @PathVariable Integer fid, @Valid @RequestBody FestRequest festRequest) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Fest fest = festRepository.findById(fid).orElse(null);
        if (fest == null || !fest.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Fest not found or not owned by this college"));
        }
        // Duplicate name check (excluding self)
        boolean exists = festRepository.findByCollege(college).stream()
                .anyMatch(f -> !f.getFid().equals(fid) && f.getFname().equalsIgnoreCase(festRequest.getFname()));
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fest name already exists for this college"));
        }
        // Date validation
        if (!isValidDateRange(festRequest.getStartDate(), festRequest.getEndDate())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Start date must be before end date"));
        }
        fest.setFname(festRequest.getFname());
        fest.setFdescription(festRequest.getFdescription());
        fest.setStartDate(festRequest.getStartDate());
        fest.setEndDate(festRequest.getEndDate());
        festRepository.save(fest);
        return ResponseEntity.ok(fest);
    }

    @DeleteMapping("/{fid}")
    public ResponseEntity<?> deleteFest(@AuthenticationPrincipal User user, @PathVariable Integer fid) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Fest fest = festRepository.findById(fid).orElse(null);
        if (fest == null || !fest.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Fest not found or not owned by this college"));
        }
        festRepository.delete(fest);
        return ResponseEntity.ok().build();
    }

    private boolean isValidDateRange(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            return s.isBefore(e) || s.isEqual(e);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid date format for start date '" + start + "' or end date '" + end + "'.");
        }
    }
} 