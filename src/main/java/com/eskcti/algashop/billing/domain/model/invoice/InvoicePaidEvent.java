package com.eskcti.algashop.billing.domain.model.invoice;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoicePaidEvent {
  private UUID invoiceId;
  private UUID customerId;
  private String orderId;
  private OffsetDateTime paidAt;
}