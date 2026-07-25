package com.eskcti.algashop.billing.domain.model.invoice;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aLineItem;
import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.anInvoice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eskcti.algashop.billing.domain.model.DomainException;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class InvoicingServiceTest {

  @Mock
  private InvoiceRepository invoiceRepository;

  @InjectMocks
  private InvoicingService invoicingService;

  @Test
  void shouldIssueInvoiceWhenOrderDoesNotExist() {
    when(invoiceRepository.existsByOrderId("order-1")).thenReturn(false);

    Invoice invoice = invoicingService.issue(
        "order-1",
        UUID.randomUUID(),
        aPayer(),
        Set.of(aLineItem()));

    assertThat(invoice.getOrderId()).isEqualTo("order-1");
    assertThat(invoice.isUnpaid()).isTrue();
  }

  @Test
  void shouldRejectDuplicateOrder() {
    when(invoiceRepository.existsByOrderId("order-1")).thenReturn(true);

    assertThatThrownBy(() -> invoicingService.issue(
        "order-1",
        UUID.randomUUID(),
        aPayer(),
        Set.of(aLineItem())))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Invoice already exists for order order-1");
  }

  @Test
  void shouldAssignPaidPayment() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();
    Payment payment = aPayment(invoice, PaymentStatus.PAID);

    invoicingService.assignPayment(invoice, payment);

    assertThat(invoice.isPaid()).isTrue();
    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gateway-123");
  }

  @Test
  void shouldAssignFailedPayment() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();
    Payment payment = aPayment(invoice, PaymentStatus.FAILED);

    invoicingService.assignPayment(invoice, payment);

    assertThat(invoice.isCanceled()).isTrue();
    assertThat(invoice.getCancelReason()).isEqualTo("Payment failed");
    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gateway-123");
  }

  @Test
  void shouldAssignRefundedPayment() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();
    Payment payment = aPayment(invoice, PaymentStatus.REFUNDED);

    invoicingService.assignPayment(invoice, payment);

    assertThat(invoice.isCanceled()).isTrue();
    assertThat(invoice.getCancelReason()).isEqualTo("Payment refunded");
  }

  @Test
  void shouldAssignPendingPaymentWithoutChangingStatus() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();
    Payment payment = aPayment(invoice, PaymentStatus.PENDING);

    invoicingService.assignPayment(invoice, payment);

    assertThat(invoice.isUnpaid()).isTrue();
    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gateway-123");
  }

  private Payment aPayment(Invoice invoice, PaymentStatus status) {
    return Payment.builder()
        .gatewayCode("gateway-123")
        .invoiceId(invoice.getId())
        .method(PaymentMethod.GATEWAY_BALANCE)
        .status(status)
        .build();
  }
}
