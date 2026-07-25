package com.eskcti.algashop.billing.domain.model.invoice.payment;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

class PaymentRequestTest {

  @Test
  void shouldCreateValidPaymentRequestWithGatewayBalance() {
    UUID invoiceId = UUID.randomUUID();

    PaymentRequest request = PaymentRequest.builder()
        .method(PaymentMethod.GATEWAY_BALANCE)
        .amount(new BigDecimal("200.00"))
        .invoiceId(invoiceId)
        .payer(aPayer())
        .build();

    assertThat(request.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(request.getAmount()).isEqualByComparingTo("200.00");
    assertThat(request.getInvoiceId()).isEqualTo(invoiceId);
    assertThat(request.getCreditCardId()).isNull();
    assertThat(request.getPayer()).isNotNull();
  }

  @Test
  void shouldCreateValidPaymentRequestWithCreditCard() {
    UUID invoiceId = UUID.randomUUID();
    UUID creditCardId = UUID.randomUUID();

    PaymentRequest request = PaymentRequest.builder()
        .method(PaymentMethod.CREDIT_CARD)
        .amount(new BigDecimal("200.00"))
        .invoiceId(invoiceId)
        .creditCardId(creditCardId)
        .payer(aPayer())
        .build();

    assertThat(request.getCreditCardId()).isEqualTo(creditCardId);
  }

  @Test
  void shouldRejectCreditCardPaymentWithoutCreditCardId() {
    assertThatThrownBy(() -> PaymentRequest.builder()
        .method(PaymentMethod.CREDIT_CARD)
        .amount(new BigDecimal("200.00"))
        .invoiceId(UUID.randomUUID())
        .payer(aPayer())
        .build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNullPayer() {
    assertThatThrownBy(() -> PaymentRequest.builder()
        .method(PaymentMethod.GATEWAY_BALANCE)
        .amount(new BigDecimal("200.00"))
        .invoiceId(UUID.randomUUID())
        .payer(null)
        .build())
        .isInstanceOf(NullPointerException.class);
  }
}
