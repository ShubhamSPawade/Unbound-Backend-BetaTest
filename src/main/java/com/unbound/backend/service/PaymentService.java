package com.unbound.backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.unbound.backend.entity.EventRegistration;
import com.unbound.backend.entity.Payment;
import com.unbound.backend.entity.Student;
import com.unbound.backend.repository.EventRegistrationRepository;
import com.unbound.backend.repository.PaymentRepository;
import com.unbound.backend.service.EmailService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EmailService emailService;

    @Value("${razorpay.keyId}")
    private String razorpayKeyId;

    @Value("${razorpay.keySecret}")
    private String razorpayKeySecret;

    public Order createOrder(EventRegistration registration, int amount, String currency, String receiptEmail) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // amount in paise
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", "reg-" + registration.getRid());
        orderRequest.put("payment_capture", 1);
        Order order = client.orders.create(orderRequest);

        Payment payment = Payment.builder()
                .eventRegistration(registration)
                .razorpayOrderId(order.get("id"))
                .status("pending")
                .amount(amount)
                .currency(currency)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .receiptEmail(receiptEmail)
                .build();
        paymentRepository.save(payment);
        return order;
    }

    public void updatePaymentStatus(String razorpayOrderId, String status, String paymentId) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getRazorpayOrderId().equals(razorpayOrderId))
                .findFirst().orElse(null);
        if (payment != null) {
            payment.setStatus(status);
            payment.setPaymentId(paymentId);
            paymentRepository.save(payment);
            // Update EventRegistration paymentStatus
            EventRegistration reg = payment.getEventRegistration();
            if (reg != null) {
                reg.setPaymentStatus(status);
                eventRegistrationRepository.save(reg);
                // Send email receipt if payment is successful
                if ("paid".equalsIgnoreCase(status) && payment.getReceiptEmail() != null) {
                    Student student = reg.getStudent();
                    String subject = "Payment Receipt - Unbound Event Registration";
                    String text = String.format("Dear %s,\n\nYour payment for event '%s' (amount: %d %s) was successful.\nPayment ID: %s\nOrder ID: %s\n\nThank you for registering!\n\n- Unbound Platform Team",
                        student.getSname(),
                        reg.getEvent().getEname(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        paymentId,
                        razorpayOrderId
                    );
                    emailService.sendEmail(payment.getReceiptEmail(), subject, text);
                }
            }
        }
    }

    // added this code: public getters for Razorpay keys
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
    public String getRazorpayKeySecret() {
        return razorpayKeySecret;
    }
    // end added code
} 
