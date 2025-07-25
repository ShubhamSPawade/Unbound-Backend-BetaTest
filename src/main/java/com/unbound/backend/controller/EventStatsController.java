package com.unbound.backend.controller;

import com.unbound.backend.entity.Event;
import com.unbound.backend.entity.Fest;
import com.unbound.backend.repository.EventRepository;
import com.unbound.backend.repository.FestRepository;
import com.unbound.backend.repository.EventRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventStatsController {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private FestRepository festRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @GetMapping("/{eventId}/stats")
    public ResponseEntity<?> getEventStats(@PathVariable Integer eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("error", "Event not found"));
        int registrationCount = eventRegistrationRepository.findByEvent(event).size();
        String eventDate = event.getEventDate();
        LocalDate today = LocalDate.now();
        LocalDate eventDay;
        try {
            eventDay = LocalDate.parse(eventDate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid event date format"));
        }
        long daysLeft = ChronoUnit.DAYS.between(today, eventDay);
        // Registration deadline: use fest endDate if linked, else event date
        String deadline = eventDay.toString();
        if (event.getFest() != null) {
            Fest fest = event.getFest();
            deadline = fest.getEndDate();
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("registrationCount", registrationCount);
        stats.put("daysLeft", daysLeft);
        stats.put("registrationDeadline", deadline);
        stats.put("eventDate", eventDate);
        return ResponseEntity.ok(stats);
    }
} 