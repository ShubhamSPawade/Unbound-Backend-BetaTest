package com.unbound.backend.service;

import com.unbound.backend.entity.*;
import com.unbound.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class EventReminderService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;
    @Autowired
    private EmailService emailService;

    // Runs every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void sendEventReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            if (tomorrow.toString().equals(event.getEventDate())) {
                List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
                for (EventRegistration reg : regs) {
                    Student student = reg.getStudent();
                    emailService.sendEmail(
                        student.getUser().getEmail(),
                        "Event Reminder - " + event.getEname(),
                        String.format("Dear %s,\n\nThis is a reminder for your upcoming event '%s'.\nEvent Date: %s\nLocation: %s\n\nSee you there!\n\n- Unbound Platform Team",
                            student.getSname(), event.getEname(), event.getEventDate(), event.getLocation())
                    );
                }
            }
        }
    }
} 