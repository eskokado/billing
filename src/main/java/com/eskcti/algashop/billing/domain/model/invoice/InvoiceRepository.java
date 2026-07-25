package com.eskcti.algashop.billing.domain.model.invoice;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
  boolean existsByOrderId(String orderId);

  Optional<Invoice> findByOrderId(String orderId);
}
