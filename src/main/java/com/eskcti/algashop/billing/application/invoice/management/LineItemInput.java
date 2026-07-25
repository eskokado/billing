package com.eskcti.algashop.billing.application.invoice.management;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LineItemInput {
  private String name;
  private BigDecimal amount;
  private Integer quantity;
}