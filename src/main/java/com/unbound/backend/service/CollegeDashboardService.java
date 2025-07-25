package com.unbound.backend.service;

import com.unbound.backend.entity.*;
import com.unbound.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CollegeDashboardService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EventReviewRepository eventReviewRepository;

    public List<Map<String, Object>> getAllCollegeEvents(College college) {
        List<Event> events = eventRepository.findByCollege(college);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Event event : events) {
            List<EventRegistration> regs = eventRegistrationRepository.findByEvent(event);
            long totalRegs = regs.size();
            long paidRegs = regs.stream().filter(r -> "paid".equalsIgnoreCase(r.getPaymentStatus())).count();
            double avgRating = 0.0;
            int reviewCount = 0;
            try {
                var reviewsList = eventReviewRepository.findByEvent(event);
                reviewCount = reviewsList.size();
                avgRating = reviewsList.stream().mapToInt(EventReview::getRating).average().orElse(0.0);
            } catch (Exception e) { throw new RuntimeException("Failed to fetch reviews: " + e.getMessage()); }
            result.add(Map.of(
                "eventId", event.getEid(),
                "eventName", event.getEname(),
                "eventDate", event.getEventDate(),
                "totalRegistrations", totalRegs,
                "paidRegistrations", paidRegs,
                "reviewCount", reviewCount,
                "averageRating", avgRating
            ));
        }
        return result;
    }

    public Map<String, Object> getCollegeDashboardStats(College college) {
        List<Event> events = eventRepository.findByCollege(college);
        int totalEvents = events.size();
        long totalRegistrations = events.stream().mapToLong(e -> eventRegistrationRepository.findByEvent(e).size()).sum();
        long totalPaid = events.stream().mapToLong(e -> eventRegistrationRepository.findByEvent(e).stream().filter(r -> "paid".equalsIgnoreCase(r.getPaymentStatus())).count()).sum();
        long totalReviews = events.stream().mapToLong(e -> eventReviewRepository.findByEvent(e).size()).sum();
        int totalRevenue = events.stream().mapToInt(e -> paymentRepository.findAll().stream().filter(p -> p.getEventRegistration().getEvent().getEid().equals(e.getEid()) && "paid".equalsIgnoreCase(p.getStatus())).mapToInt(p -> p.getAmount()).sum()).sum();
        return Map.of(
            "totalEvents", totalEvents,
            "totalRegistrations", totalRegistrations,
            "totalPaid", totalPaid,
            "totalReviews", totalReviews,
            "totalRevenue", totalRevenue
        );
    }
} 