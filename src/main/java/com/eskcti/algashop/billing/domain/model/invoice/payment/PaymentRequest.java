package com.eskcti.algashop.billing.domain.model.invoice.payment;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.eskcti.algashop.billing.domain.model.invoice.Payer;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@Builder
public class PaymentRequest {
  private PaymentMethod method;
  private BigDecimal amount;
  private UUID invoiceId;
  private UUID creditCardId;
  private Payer payer;

  public PaymentRequest(PaymentMethod method,
      BigDecimal amount, UUID invoiceId,
      UUID creditCardId, Payer payer) {
    Objects.requireNonNull(method);
    Objects.requireNonNull(amount);
    Objects.requireNonNull(invoiceId);
    Objects.requireNonNull(payer);

    if (method.equals(PaymentMethod.CREDIT_CARD)) {
      Objects.requireNonNull(creditCardId);
    }

    this.method = method;
    this.amount = amount;
    this.invoiceId = invoiceId;
    this.creditCardId = creditCardId;
    this.payer = payer;
  }
}