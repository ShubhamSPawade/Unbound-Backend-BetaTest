package com.unbound.backend.controller;

import com.unbound.backend.entity.Event;
import com.unbound.backend.entity.Fest;
import com.unbound.backend.entity.College;
import com.unbound.backend.repository.EventRepository;
import com.unbound.backend.repository.FestRepository;
import com.unbound.backend.repository.CollegeRepository;
import com.unbound.backend.repository.EventRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

@RestController
@RequestMapping("/api/explore")
public class ExploreController {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private FestRepository festRepository;
    @Autowired
    private CollegeRepository collegeRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @GetMapping("/fests")
    public ResponseEntity<?> exploreFests(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        List<Fest> fests = festRepository.findAll();
        if (name != null) {
            fests = fests.stream().filter(f -> f.getFname().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
        }
        if (college != null) {
            fests = fests.stream().filter(f -> f.getCollege().getCname().toLowerCase().contains(college.toLowerCase())).collect(Collectors.toList());
        }
        if (startDate != null) {
            fests = fests.stream().filter(f -> f.getStartDate().compareTo(startDate) >= 0).collect(Collectors.toList());
        }
        if (endDate != null) {
            fests = fests.stream().filter(f -> f.getEndDate().compareTo(endDate) <= 0).collect(Collectors.toList());
        }
        return ResponseEntity.ok(fests);
    }

    @GetMapping("/events")
    public ResponseEntity<?> exploreEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String entryFee, // "free" or "paid"
            @RequestParam(required = false) Boolean team,
            @RequestParam(required = false) String festName,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false, defaultValue = "date_asc") String sort // "date_asc", "date_desc", "popularity", "fee_asc", "fee_desc"
    ) {
        List<Event> events = eventRepository.findAll();
        if (category != null) {
            events = events.stream().filter(e -> category.equalsIgnoreCase(e.getCategory())).collect(Collectors.toList());
        }
        if (mode != null) {
            events = events.stream().filter(e -> mode.equalsIgnoreCase(e.getMode())).collect(Collectors.toList());
        }
        if (date != null) {
            events = events.stream().filter(e -> e.getEventDate().equals(date)).collect(Collectors.toList());
        }
        if (entryFee != null) {
            if (entryFee.equalsIgnoreCase("free")) {
                events = events.stream().filter(e -> e.getFees() == 0).collect(Collectors.toList());
            } else if (entryFee.equalsIgnoreCase("paid")) {
                events = events.stream().filter(e -> e.getFees() > 0).collect(Collectors.toList());
            }
        }
        if (team != null) {
            events = events.stream().filter(e -> e.getTeamIsAllowed().equals(team)).collect(Collectors.toList());
        }
        if (festName != null) {
            events = events.stream().filter(e -> e.getFest() != null && e.getFest().getFname().toLowerCase().contains(festName.toLowerCase())).collect(Collectors.toList());
        }
        if (college != null) {
            events = events.stream().filter(e -> e.getCollege().getCname().toLowerCase().contains(college.toLowerCase())).collect(Collectors.toList());
        }
        if (location != null) {
            events = events.stream().filter(e -> e.getLocation() != null && e.getLocation().toLowerCase().contains(location.toLowerCase())).collect(Collectors.toList());
        }
        // Sorting
        switch (sort) {
            case "date_desc":
                events = events.stream().sorted((a, b) -> b.getEventDate().compareTo(a.getEventDate())).collect(Collectors.toList());
                break;
            case "popularity":
                // Sort by number of registrations (descending)
                events = events.stream().sorted((a, b) -> Integer.compare(
                    eventRegistrationRepository.findByEvent(b).size(),
                    eventRegistrationRepository.findByEvent(a).size()
                )).collect(Collectors.toList());
                break;
            case "fee_asc":
                events = events.stream().sorted(Comparator.comparingInt(Event::getFees)).collect(Collectors.toList());
                break;
            case "fee_desc":
                events = events.stream().sorted((a, b) -> Integer.compare(b.getFees(), a.getFees())).collect(Collectors.toList());
                break;
            default:
                // date_asc
                events = events.stream().sorted((a, b) -> a.getEventDate().compareTo(b.getEventDate())).collect(Collectors.toList());
        }
        return ResponseEntity.ok(events);
    }
} 