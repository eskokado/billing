package com.eskcti.algashop.billing.infrastructure.payment;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import com.eskcti.algashop.billing.infrastructure.payment.fake.PaymentGatewayServiceFakeImpl;

class PaymentGatewayServiceFakeImplTest {

  private final PaymentGatewayServiceFakeImpl service = new PaymentGatewayServiceFakeImpl();

  @Test
  void shouldCapturePayment() {
    UUID invoiceId = UUID.randomUUID();
    PaymentRequest request = PaymentRequest.builder()
        .method(PaymentMethod.CREDIT_CARD)
        .amount(new BigDecimal("200.00"))
        .invoiceId(invoiceId)
        .creditCardId(UUID.randomUUID())
        .payer(aPayer())
        .build();

    Payment payment = service.capture(request);

    assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
    assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getGatewayCode()).isNotBlank();
  }

  @Test
  void shouldFindPaymentByCode() {
    Payment payment = service.findByCode("gateway-code");

    assertThat(payment.getInvoiceId()).isNotNull();
    assertThat(payment.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getGatewayCode()).isNotBlank();
  }
}
