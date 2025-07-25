package com.unbound.backend.controller;

import com.unbound.backend.dto.EventRegistrationRequest;
import com.unbound.backend.entity.*;
import com.unbound.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.unbound.backend.repository.EventReviewRepository;
import com.unbound.backend.service.StudentDashboardService;
import com.unbound.backend.service.EmailService;
import com.unbound.backend.service.CertificateService;

@RestController
@RequestMapping("/api/student/events")
public class StudentEventController {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMembersRepository teamMembersRepository;
    @Autowired
    private EventReviewRepository eventReviewRepository;
    @Autowired
    private StudentDashboardService studentDashboardService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private CertificateService certificateService;

    private Student getStudentForUser(User user) {
        return studentRepository.findAll().stream()
                .filter(s -> s.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerForEvent(@AuthenticationPrincipal User user, @RequestBody EventRegistrationRequest req) {
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can register for events"));
        }
        Student student = getStudentForUser(user);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        Optional<Event> eventOpt = eventRepository.findById(req.getEventId());
        if (eventOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Event not found"));
        Event event = eventOpt.get();
        // Check for duplicate registration
        if (eventRegistrationRepository.findByEventAndStudent(event, student).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already registered for this event"));
        }
        // Check event capacity
        long regCount = eventRegistrationRepository.findByEvent(event).stream().count();
        if (regCount >= event.getCapacity()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Event is full"));
        }
        // Solo registration
        if ("solo".equalsIgnoreCase(req.getRegistrationType())) {
            if (event.getTeamIsAllowed()) {
                return ResponseEntity.badRequest().body(Map.of("error", "This event requires team registration"));
            }
            EventRegistration registration = EventRegistration.builder()
                    .event(event)
                    .student(student)
                    .erdateTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME))
                    .status("registered")
                    .paymentStatus(event.getFees() > 0 ? "pending" : "paid")
                    .build();
            eventRegistrationRepository.save(registration);
            // Send registration confirmation email
            emailService.sendEmail(
                student.getUser().getEmail(),
                "Registration Confirmation - " + event.getEname(),
                String.format("Dear %s,\n\nYou have successfully registered for event '%s'.\nEvent Date: %s\nLocation: %s\n\nThank you for registering!\n\n- Unbound Platform Team",
                    student.getSname(), event.getEname(), event.getEventDate(), event.getLocation())
            );
            return ResponseEntity.ok(Map.of("message", "Registered successfully (solo)", "registration", registration));
        }
        // Team registration
        if ("team".equalsIgnoreCase(req.getRegistrationType())) {
            if (!event.getTeamIsAllowed()) {
                return ResponseEntity.badRequest().body(Map.of("error", "This event does not allow team registration"));
            }
            Team team = null;
            // Join existing team
            if (req.getTeamId() != null) {
                Optional<Team> teamOpt = teamRepository.findById(req.getTeamId());
                if (teamOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Team not found"));
                team = teamOpt.get();
                // Check if already a member
                boolean alreadyMember = teamMembersRepository.findByTeam(team).stream()
                        .anyMatch(tm -> tm.getStudent().getSid().equals(student.getSid()));
                if (alreadyMember) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Already a member of this team"));
                }
                // Add to team
                TeamMembers tm = TeamMembers.builder().team(team).student(student).build();
                teamMembersRepository.save(tm);
            } else if (req.getTeamName() != null && req.getMemberIds() != null && !req.getMemberIds().isEmpty()) {
                // Create new team
                team = Team.builder().event(event).tname(req.getTeamName()).creator(student).build();
                team = teamRepository.save(team);
                // Add members
                Set<Integer> uniqueMemberIds = new HashSet<>(req.getMemberIds());
                for (Integer sid : uniqueMemberIds) {
                    Optional<Student> memberOpt = studentRepository.findById(sid);
                    if (memberOpt.isEmpty()) continue;
                    TeamMembers tm = TeamMembers.builder().team(team).student(memberOpt.get()).build();
                    teamMembersRepository.save(tm);
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid team registration request"));
            }
            // Register the student (and team) for the event
            EventRegistration registration = EventRegistration.builder()
                    .event(event)
                    .student(student)
                    .team(team)
                    .erdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .status("registered")
                    .build();
            eventRegistrationRepository.save(registration);
            // Send registration confirmation email
            emailService.sendEmail(
                student.getUser().getEmail(),
                "Registration Confirmation - " + event.getEname(),
                String.format("Dear %s,\n\nYou have successfully registered for event '%s' as part of a team.\nEvent Date: %s\nLocation: %s\n\nThank you for registering!\n\n- Unbound Platform Team",
                    student.getSname(), event.getEname(), event.getEventDate(), event.getLocation())
            );
            return ResponseEntity.ok(Map.of("message", "Registered successfully (team)", "registration", registration));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid registration type"));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myRegistrations(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can view their registrations"));
        }
        Student student = getStudentForUser(user);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        return ResponseEntity.ok(studentDashboardService.getMyRegistrations(student));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getStudentDashboardStats(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can view dashboard stats"));
        }
        Student student = getStudentForUser(user);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        return ResponseEntity.ok(studentDashboardService.getStudentDashboardStats(student));
    }

    @GetMapping("/{eventId}/certificate")
    public ResponseEntity<?> downloadCertificate(@AuthenticationPrincipal User user, @PathVariable Integer eventId) {
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can download certificates"));
        }
        Student student = getStudentForUser(user);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Event not found"));
        Event event = eventOpt.get();
        Optional<EventRegistration> regOpt = eventRegistrationRepository.findByEventAndStudent(event, student);
        if (regOpt.isEmpty() || (!"paid".equalsIgnoreCase(regOpt.get().getPaymentStatus()) && event.getFees() > 0)) {
            return ResponseEntity.status(403).body(Map.of("error", "You must be a registered and paid participant to download certificate"));
        }
        if (!regOpt.get().isCertificateApproved()) {
            return ResponseEntity.status(403).body(Map.of("error", "Certificate not yet approved by college"));
        }
        // Only after event is completed
        try {
            if (!java.time.LocalDate.now().isAfter(java.time.LocalDate.parse(event.getEventDate()))) {
                return ResponseEntity.status(403).body(Map.of("error", "Certificate available only after event completion"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid event date for event ID " + eventId + ": " + e.getMessage());
        }
        try {
            byte[] pdf = certificateService.generateCertificate(
                student.getSname(),
                event.getEname(),
                event.getFest() != null ? event.getFest().getFname() : null,
                event.getEventDate()
            );
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=certificate.pdf")
                .body(pdf);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certificate for student ID " + student.getSid() + ", event ID " + eventId + ": " + e.getMessage());
        }
    }
} 