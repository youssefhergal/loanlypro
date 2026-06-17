package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
}
