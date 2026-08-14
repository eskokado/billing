package com.eskcti.algashop.billing.application.invoice.management;

import java.util.UUID;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSettingsInput {
  @NotNull(message = "Payment method is required")
  private PaymentMethod method;
  private UUID creditCardId;
}
