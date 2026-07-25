package com.eskcti.algashop.billing.domain.model.invoice.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

class PaymentTest {

  @Test
  void shouldCreateValidPayment() {
    UUID invoiceId = UUID.randomUUID();

    Payment payment = Payment.builder()
        .gatewayCode("gateway-123")
        .invoiceId(invoiceId)
        .method(PaymentMethod.GATEWAY_BALANCE)
        .status(PaymentStatus.PAID)
        .build();

    assertThat(payment.getGatewayCode()).isEqualTo("gateway-123");
    assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
    assertThat(payment.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void shouldRejectBlankGatewayCode() {
    assertThatThrownBy(() -> Payment.builder()
        .gatewayCode(" ")
        .invoiceId(UUID.randomUUID())
        .method(PaymentMethod.GATEWAY_BALANCE)
        .status(PaymentStatus.PAID)
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullInvoiceId() {
    assertThatThrownBy(() -> Payment.builder()
        .gatewayCode("gateway-123")
        .invoiceId(null)
        .method(PaymentMethod.GATEWAY_BALANCE)
        .status(PaymentStatus.PAID)
        .build())
        .isInstanceOf(NullPointerException.class);
  }
}
