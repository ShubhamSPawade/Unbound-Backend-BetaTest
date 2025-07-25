package com.unbound.backend.repository;

import com.unbound.backend.entity.Payment;
import com.unbound.backend.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByEventRegistration(EventRegistration eventRegistration);
} 