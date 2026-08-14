package com.eskcti.algashop.billing.application.invoice.management;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LineItemInput {
  @NotBlank(message = "Name is required")
  private String name;
  @NotNull(message = "Amount is required")
  private BigDecimal amount;
}