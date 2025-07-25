package com.unbound.backend.controller;

import com.unbound.backend.dto.EventRequest;
import com.unbound.backend.entity.Event;
import com.unbound.backend.entity.College;
import com.unbound.backend.entity.Fest;
import com.unbound.backend.entity.User;
import com.unbound.backend.repository.EventRepository;
import com.unbound.backend.repository.CollegeRepository;
import com.unbound.backend.repository.FestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Image;

@RestController
@RequestMapping("/api/events")
public class EventController {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CollegeRepository collegeRepository;
    @Autowired
    private FestRepository festRepository;

    private College getCollegeForUser(User user) {
        return collegeRepository.findAll().stream()
                .filter(c -> c.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> listEvents(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        List<Event> events = eventRepository.findByCollege(college);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@AuthenticationPrincipal User user, @Valid @RequestBody EventRequest eventRequest) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        // Duplicate name check
        boolean exists = eventRepository.findByCollege(college).stream()
                .anyMatch(e -> e.getEname().equalsIgnoreCase(eventRequest.getEname()));
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "Event name already exists for this college"));
        }
        // Fest linkage and event date validation
        Fest fest = null;
        if (eventRequest.getFestId() != null) {
            fest = festRepository.findById(eventRequest.getFestId()).orElse(null);
            if (fest == null || !fest.getCollege().getCid().equals(college.getCid())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid fest for this college"));
            }
            if (!isDateWithinRange(eventRequest.getEventDate(), fest.getStartDate(), fest.getEndDate())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Event date must be within fest date range"));
            }
        }
        Event event = Event.builder()
                .college(college)
                .fest(fest)
                .ename(eventRequest.getEname())
                .edescription(eventRequest.getEdescription())
                .eventDate(eventRequest.getEventDate())
                .fees(eventRequest.getFees())
                .location(eventRequest.getLocation())
                .capacity(eventRequest.getCapacity())
                .teamIsAllowed(eventRequest.getTeamIsAllowed())
                .category(eventRequest.getCategory())
                .mode(eventRequest.getMode())
                .posterUrl(eventRequest.getPosterUrl())
                .build();
        Event saved = eventRepository.save(event);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{eid}")
    public ResponseEntity<?> updateEvent(@AuthenticationPrincipal User user, @PathVariable Integer eid, @Valid @RequestBody EventRequest eventRequest) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        // Duplicate name check (excluding self)
        boolean exists = eventRepository.findByCollege(college).stream()
                .anyMatch(e -> !e.getEid().equals(eid) && e.getEname().equalsIgnoreCase(eventRequest.getEname()));
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "Event name already exists for this college"));
        }
        // Fest linkage and event date validation
        Fest fest = null;
        if (eventRequest.getFestId() != null) {
            fest = festRepository.findById(eventRequest.getFestId()).orElse(null);
            if (fest == null || !fest.getCollege().getCid().equals(college.getCid())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid fest for this college"));
            }
            if (!isDateWithinRange(eventRequest.getEventDate(), fest.getStartDate(), fest.getEndDate())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Event date must be within fest date range"));
            }
        }
        event.setFest(fest);
        event.setEname(eventRequest.getEname());
        event.setEdescription(eventRequest.getEdescription());
        event.setEventDate(eventRequest.getEventDate());
        event.setFees(eventRequest.getFees());
        event.setLocation(eventRequest.getLocation());
        event.setCapacity(eventRequest.getCapacity());
        event.setTeamIsAllowed(eventRequest.getTeamIsAllowed());
        event.setCategory(eventRequest.getCategory());
        event.setMode(eventRequest.getMode());
        event.setPosterUrl(eventRequest.getPosterUrl());
        eventRepository.save(event);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{eid}")
    public ResponseEntity<?> deleteEvent(@AuthenticationPrincipal User user, @PathVariable Integer eid) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        eventRepository.delete(event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{eid}/poster")
    public ResponseEntity<?> uploadEventPoster(@AuthenticationPrincipal User user, @PathVariable Integer eid, @RequestParam("file") MultipartFile file) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can upload posters"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        try {
            // File type/size validation
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
            }
            if (file.getSize() > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.badRequest().body(Map.of("error", "File size must be less than 5MB"));
            }
            String uploadDir = "uploads/posters/";
            Files.createDirectories(Paths.get(uploadDir));
            String filename = "event_" + eid + "_" + System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = Paths.get(uploadDir, filename);
            Files.write(filePath, file.getBytes());
            String posterUrl = "/" + uploadDir + filename;
            // Generate thumbnail
            String thumbFilename = "thumb_" + filename;
            Path thumbPath = Paths.get(uploadDir, thumbFilename);
            try {
                BufferedImage originalImage = ImageIO.read(file.getInputStream());
                int width = 300;
                int height = (int) (originalImage.getHeight() * (300.0 / originalImage.getWidth()));
                Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                BufferedImage thumbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                thumbImage.getGraphics().drawImage(tmp, 0, 0, null);
                ImageIO.write(thumbImage, "jpg", thumbPath.toFile());
            } catch (Exception e) { throw new RuntimeException("Failed to generate thumbnail for event ID " + eid + ": " + e.getMessage()); }
            String posterThumbnailUrl = "/" + uploadDir + thumbFilename;
            event.setPosterUrl(posterUrl);
            event.setPosterThumbnailUrl(posterThumbnailUrl);
            event.setPosterApproved(false); // Needs admin approval
            eventRepository.save(event);
            return ResponseEntity.ok(Map.of("posterUrl", posterUrl, "posterThumbnailUrl", posterThumbnailUrl));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload poster for event ID " + eid + ": " + e.getMessage());
        }
    }

    @PostMapping("/{eid}/poster/approve")
    public ResponseEntity<?> approveEventPoster(@AuthenticationPrincipal User user, @PathVariable Integer eid) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can approve posters"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        event.setPosterApproved(true);
        eventRepository.save(event);
        return ResponseEntity.ok(Map.of("message", "Poster approved"));
    }

    @PostMapping("/{eid}/poster/reject")
    public ResponseEntity<?> rejectEventPoster(@AuthenticationPrincipal User user, @PathVariable Integer eid, @RequestParam String reason) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can reject posters"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        event.setPosterApproved(false);
        eventRepository.save(event);
        return ResponseEntity.ok(Map.of("message", "Poster rejected"));
    }

    @DeleteMapping("/{eid}/poster")
    public ResponseEntity<?> deleteEventPoster(@AuthenticationPrincipal User user, @PathVariable Integer eid) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can delete posters"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        try {
            if (event.getPosterUrl() != null) {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(event.getPosterUrl().replaceFirst("^/", "")));
            }
            if (event.getPosterThumbnailUrl() != null) {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(event.getPosterThumbnailUrl().replaceFirst("^/", "")));
            }
        } catch (Exception e) { throw new RuntimeException("Failed to delete poster file for event ID " + eid + ": " + e.getMessage()); }
        event.setPosterUrl(null);
        event.setPosterThumbnailUrl(null);
        event.setPosterApproved(false);
        eventRepository.save(event);
        return ResponseEntity.ok(Map.of("message", "Poster deleted"));
    }

    @GetMapping("/{eid}/poster/audit-logs")
    public ResponseEntity<?> getPosterAuditLogs(@AuthenticationPrincipal User user, @PathVariable Integer eid) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only colleges can view audit logs"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.notFound().build();
        Event event = eventRepository.findById(eid).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        return ResponseEntity.ok(null); // No audit logs to return as they are removed
    }

    private boolean isDateWithinRange(String date, String start, String end) {
        try {
            LocalDate d = LocalDate.parse(date);
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            return (d.isEqual(s) || d.isAfter(s)) && (d.isEqual(e) || d.isBefore(e));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid date format for event date '" + date + "', start '" + start + "', end '" + end + "'.");
        }
    }
} 