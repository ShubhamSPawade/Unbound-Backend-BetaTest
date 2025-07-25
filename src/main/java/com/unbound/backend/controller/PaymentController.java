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

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

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

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> req) {
        String razorpayOrderId = (String) req.get("razorpayOrderId");
        String status = (String) req.get("status");
        String paymentId = (String) req.get("paymentId");
        paymentService.updatePaymentStatus(razorpayOrderId, status, paymentId);
        return ResponseEntity.ok(Map.of("message", "Payment status updated"));
    }
} 