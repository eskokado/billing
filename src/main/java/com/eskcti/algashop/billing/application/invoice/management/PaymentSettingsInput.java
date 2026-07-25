package com.eskcti.algashop.billing.application.invoice.management;

import java.util.UUID;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSettingsInput {
  private PaymentMethod method;
  private UUID creditCardId;
}