package com.unbound.backend.controller;

import com.unbound.backend.entity.College;
import com.unbound.backend.entity.Event;
import com.unbound.backend.entity.Payment;
import com.unbound.backend.entity.EventRegistration;
import com.unbound.backend.repository.CollegeRepository;
import com.unbound.backend.repository.EventRepository;
import com.unbound.backend.repository.PaymentRepository;
import com.unbound.backend.repository.EventRegistrationRepository;
import com.unbound.backend.entity.User;
import com.unbound.backend.entity.Fest;
import com.unbound.backend.repository.FestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import com.unbound.backend.entity.EventReview;
import com.unbound.backend.repository.EventReviewRepository;
import com.unbound.backend.service.CollegeDashboardService;

@RestController
@RequestMapping("/api/college/dashboard")
public class CollegeDashboardController {
    @Autowired
    private CollegeRepository collegeRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;
    @Autowired
    private FestRepository festRepository;
    @Autowired
    private EventReviewRepository eventReviewRepository;
    @Autowired
    private CollegeDashboardService collegeDashboardService;

    private College getCollegeForUser(User user) {
        return collegeRepository.findAll().stream()
                .filter(c -> c.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
    }

    @GetMapping("/earnings")
    public ResponseEntity<?> getTotalEarnings(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        List<Event> events = eventRepository.findByCollege(college);
        List<Payment> payments = events.stream()
                .flatMap(e -> paymentRepository.findAll().stream()
                        .filter(p -> p.getEventRegistration().getEvent().getEid().equals(e.getEid()) && "paid".equalsIgnoreCase(p.getStatus())))
                .collect(Collectors.toList());
        int totalEarnings = payments.stream().mapToInt(Payment::getAmount).sum();
        Map<String, Object> breakdown = new HashMap<>();
        for (Event event : events) {
            int eventEarnings = payments.stream()
                    .filter(p -> p.getEventRegistration().getEvent().getEid().equals(event.getEid()))
                    .mapToInt(Payment::getAmount).sum();
            breakdown.put(event.getEname(), eventEarnings);
        }
        return ResponseEntity.ok(Map.of(
                "totalEarnings", totalEarnings,
                "breakdown", breakdown
        ));
    }

    @GetMapping("/registrations")
    public ResponseEntity<?> getRegistrationStats(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        List<Event> events = eventRepository.findByCollege(college);
        List<EventRegistration> allRegs = events.stream()
                .flatMap(e -> eventRegistrationRepository.findByEvent(e).stream())
                .collect(Collectors.toList());
        int totalRegistrations = allRegs.size();
        long paidRegistrations = allRegs.stream().filter(r -> "paid".equalsIgnoreCase(r.getPaymentStatus())).count();
        long unpaidRegistrations = allRegs.stream().filter(r -> !"paid".equalsIgnoreCase(r.getPaymentStatus())).count();
        Map<String, Object> eventWise = new HashMap<>();
        for (Event event : events) {
            long eventTotal = allRegs.stream().filter(r -> r.getEvent().getEid().equals(event.getEid())).count();
            long eventPaid = allRegs.stream().filter(r -> r.getEvent().getEid().equals(event.getEid()) && "paid".equalsIgnoreCase(r.getPaymentStatus())).count();
            long eventUnpaid = allRegs.stream().filter(r -> r.getEvent().getEid().equals(event.getEid()) && !"paid".equalsIgnoreCase(r.getPaymentStatus())).count();
            eventWise.put(event.getEname(), Map.of(
                "total", eventTotal,
                "paid", eventPaid,
                "unpaid", eventUnpaid
            ));
        }
        return ResponseEntity.ok(Map.of(
            "totalRegistrations", totalRegistrations,
            "paidRegistrations", paidRegistrations,
            "unpaidRegistrations", unpaidRegistrations,
            "eventWise", eventWise
        ));
    }

    @GetMapping("/analytics/by-fest")
    public ResponseEntity<?> getStatsByFest(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        List<Fest> fests = festRepository.findByCollege(college);
        Map<String, Object> festStats = new HashMap<>();
        for (Fest fest : fests) {
            List<Event> festEvents = eventRepository.findByFest(fest);
            int festRegistrations = 0;
            int festEarnings = 0;
            for (Event event : festEvents) {
                List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
                festRegistrations += regs.size();
                festEarnings += paymentRepository.findAll().stream()
                        .filter(p -> p.getEventRegistration().getEvent().getEid().equals(event.getEid()) && "paid".equalsIgnoreCase(p.getStatus()))
                        .mapToInt(Payment::getAmount).sum();
            }
            festStats.put(fest.getFname(), Map.of(
                "registrations", festRegistrations,
                "earnings", festEarnings
            ));
        }
        return ResponseEntity.ok(festStats);
    }

    @GetMapping("/analytics/by-date")
    public ResponseEntity<?> getStatsByDate(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        List<Event> events = eventRepository.findByCollege(college);
        Map<String, Map<String, Object>> dateStats = new HashMap<>();
        for (Event event : events) {
            String date = event.getEventDate();
            dateStats.putIfAbsent(date, new HashMap<>());
            Map<String, Object> stats = dateStats.get(date);
            int regCount = eventRegistrationRepository.findByEvent(event).size();
            int earnings = paymentRepository.findAll().stream()
                    .filter(p -> p.getEventRegistration().getEvent().getEid().equals(event.getEid()) && "paid".equalsIgnoreCase(p.getStatus()))
                    .mapToInt(Payment::getAmount).sum();
            stats.put("registrations", ((int) stats.getOrDefault("registrations", 0)) + regCount);
            stats.put("earnings", ((int) stats.getOrDefault("earnings", 0)) + earnings);
        }
        return ResponseEntity.ok(dateStats);
    }

    @GetMapping("/analytics/top-events")
    public ResponseEntity<?> getTopEvents(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        List<Event> events = eventRepository.findByCollege(college);
        List<Map<String, Object>> eventStats = new ArrayList<>();
        for (Event event : events) {
            int regCount = eventRegistrationRepository.findByEvent(event).size();
            int earnings = paymentRepository.findAll().stream()
                    .filter(p -> p.getEventRegistration().getEvent().getEid().equals(event.getEid()) && "paid".equalsIgnoreCase(p.getStatus()))
                    .mapToInt(Payment::getAmount).sum();
            eventStats.add(Map.of(
                "eventName", event.getEname(),
                "registrations", regCount,
                "earnings", earnings
            ));
        }
        // Top 5 by registrations
        List<Map<String, Object>> topByRegistrations = eventStats.stream()
                .sorted((a, b) -> Integer.compare((int) b.get("registrations"), (int) a.get("registrations")))
                .limit(5)
                .collect(Collectors.toList());
        // Top 5 by earnings
        List<Map<String, Object>> topByEarnings = eventStats.stream()
                .sorted((a, b) -> Integer.compare((int) b.get("earnings"), (int) a.get("earnings")))
                .limit(5)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
            "topByRegistrations", topByRegistrations,
            "topByEarnings", topByEarnings
        ));
    }

    @GetMapping("/events/{eventId}/registrations")
    public ResponseEntity<?> getEventRegistrations(@AuthenticationPrincipal User user, @PathVariable Integer eventId) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
        List<Map<String, Object>> result = new ArrayList<>();
        for (EventRegistration reg : regs) {
            Map<String, Object> regInfo = new HashMap<>();
            regInfo.put("registrationId", reg.getRid());
            regInfo.put("studentName", reg.getStudent().getSname());
            regInfo.put("teamId", reg.getTeam() != null ? reg.getTeam().getTid() : null);
            regInfo.put("teamName", reg.getTeam() != null ? reg.getTeam().getTname() : null);
            regInfo.put("paymentStatus", reg.getPaymentStatus());
            regInfo.put("registrationDate", reg.getErdateTime());
            result.add(regInfo);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/events/{eventId}/registrations/{registrationId}/approve-certificate")
    public ResponseEntity<?> approveCertificate(@AuthenticationPrincipal User user, @PathVariable Integer eventId, @PathVariable Integer registrationId) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can approve certificates"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        Optional<EventRegistration> regOpt = eventRegistrationRepository.findById(registrationId);
        if (regOpt.isEmpty() || !regOpt.get().getEvent().getEid().equals(eventId)) {
            return ResponseEntity.status(404).body(Map.of("error", "Registration not found for this event"));
        }
        EventRegistration reg = regOpt.get();
        reg.setCertificateApproved(true);
        eventRegistrationRepository.save(reg);
        return ResponseEntity.ok(Map.of("message", "Certificate approved for registrationId " + registrationId));
    }

    @PostMapping("/events/{eventId}/registrations/approve-all-certificates")
    public ResponseEntity<?> approveAllCertificates(@AuthenticationPrincipal User user, @PathVariable Integer eventId) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can approve certificates"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
        for (EventRegistration reg : regs) {
            reg.setCertificateApproved(true);
        }
        eventRegistrationRepository.saveAll(regs);
        return ResponseEntity.ok(Map.of("message", "Certificates approved for all registrations in eventId " + eventId));
    }

    @PostMapping("/events/{eventId}/registrations/approve-certificates")
    public ResponseEntity<?> approveCertificatesForList(@AuthenticationPrincipal User user, @PathVariable Integer eventId, @RequestBody Map<String, List<Integer>> req) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can approve certificates"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !event.getCollege().getCid().equals(college.getCid())) {
            return ResponseEntity.status(404).body(Map.of("error", "Event not found or not owned by this college"));
        }
        List<Integer> registrationIds = req.getOrDefault("registrationIds", List.of());
        int approved = 0;
        for (Integer regId : registrationIds) {
            Optional<EventRegistration> regOpt = eventRegistrationRepository.findById(regId);
            if (regOpt.isPresent() && regOpt.get().getEvent().getEid().equals(eventId)) {
                EventRegistration reg = regOpt.get();
                reg.setCertificateApproved(true);
                eventRegistrationRepository.save(reg);
                approved++;
            }
        }
        return ResponseEntity.ok(Map.of("message", "Certificates approved for " + approved + " registrations in eventId " + eventId));
    }

    @GetMapping("/events")
    public ResponseEntity<?> getAllCollegeEvents(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        return ResponseEntity.ok(collegeDashboardService.getAllCollegeEvents(college));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getCollegeDashboardStats(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.College) {
            return ResponseEntity.status(403).body(Map.of("error", "Only colleges can access this endpoint"));
        }
        College college = getCollegeForUser(user);
        if (college == null) return ResponseEntity.status(404).body(Map.of("error", "College not found"));
        return ResponseEntity.ok(collegeDashboardService.getCollegeDashboardStats(college));
    }
} 