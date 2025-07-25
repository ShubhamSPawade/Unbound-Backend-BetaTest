package com.unbound.backend.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pid;

    @ManyToOne
    @JoinColumn(name = "rid", referencedColumnName = "rid", nullable = false)
    private EventRegistration eventRegistration;

    @Column(nullable = false)
    private String razorpayOrderId;

    @Column(nullable = false)
    private String status; // pending, paid, failed

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String createdAt;

    @Column
    private String paymentId; // Razorpay payment id (after success)

    @Column
    private String receiptEmail;
} 