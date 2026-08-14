package com.eskcti.algashop.billing.application.invoice.management;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateInvoiceInput {
  @NotBlank(message = "Order ID is required")
  private String orderId;

  @NotNull(message = "Customer ID is required")
  private UUID customerId;

  @NotNull(message = "Payment settings is required")
  @Valid
  private PaymentSettingsInput paymentSettings;

  @NotNull(message = "Payer is required")
  @Valid
  private PayerData payer;

  @NotEmpty(message = "Items are required")
  @Valid
  private Set<LineItemInput> items;
}
