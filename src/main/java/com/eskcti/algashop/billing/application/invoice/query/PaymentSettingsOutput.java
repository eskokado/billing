package com.eskcti.algashop.billing.application.invoice.query;

import java.util.UUID;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSettingsOutput {
  private UUID id;
  private UUID creditCardId;
  private PaymentMethod method;
}