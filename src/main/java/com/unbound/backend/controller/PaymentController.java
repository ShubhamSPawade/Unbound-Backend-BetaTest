package com.unbound.backend.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayException;
import com.unbound.backend.entity.EventRegistration;
import com.unbound.backend.repository.EventRegistrationRepository;
import com.unbound.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.unbound.backend.entity.User;
import com.unbound.backend.entity.Event;
import com.unbound.backend.repository.EventRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;
    // added this code: inject EventRepository for event lookup
    @Autowired
    private EventRepository eventRepository;
    // end added code

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> req) {
        try {
            Integer registrationId = (Integer) req.get("registrationId");
            Integer amount = (Integer) req.get("amount");
            String currency = (String) req.getOrDefault("currency", "INR");
            String receiptEmail = (String) req.get("receiptEmail");
            EventRegistration registration = eventRegistrationRepository.findById(registrationId).orElse(null);
            if (registration == null) {
                throw new RuntimeException("Invalid registration ID: " + registrationId);
            }
            Order order = paymentService.createOrder(registration, amount, currency, receiptEmail);
            return ResponseEntity.ok(Map.of("order", order.toJson()));
        } catch (RazorpayException e) {
            throw new RuntimeException("Payment gateway error for registration ID " + req.get("registrationId") + ": " + e.getMessage());
        }
    }

    // added this code: create Razorpay order for event before registration
    @PostMapping("/create-order-for-event")
    public ResponseEntity<?> createOrderForEvent(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> req) {
        try {
            Integer eventId = (Integer) req.get("eventId");
            Integer amount = (Integer) req.get("amount");
            String currency = (String) req.getOrDefault("currency", "INR");
            String receiptEmail = (String) req.get("receiptEmail");
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid event ID: " + eventId));
            }
            // Create Razorpay order (no registration yet, so use eventId in receipt)
            com.razorpay.RazorpayClient client = new com.razorpay.RazorpayClient(paymentService.getRazorpayKeyId(), paymentService.getRazorpayKeySecret());
            org.json.JSONObject orderRequest = new org.json.JSONObject();
            orderRequest.put("amount", amount * 100); // amount in paise
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "event-" + eventId);
            orderRequest.put("payment_capture", 1);
            com.razorpay.Order order = client.orders.create(orderRequest);
            return ResponseEntity.ok(Map.of("order", order.toJson()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Payment gateway error: " + e.getMessage()));
        }
    }
    // end added code

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> req) {
        String razorpayOrderId = (String) req.get("razorpayOrderId");
        String status = (String) req.get("status");
        String paymentId = (String) req.get("paymentId");
        paymentService.updatePaymentStatus(razorpayOrderId, status, paymentId);
        return ResponseEntity.ok(Map.of("message", "Payment status updated"));
    }
} 
