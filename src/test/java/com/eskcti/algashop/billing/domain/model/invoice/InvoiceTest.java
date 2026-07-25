package com.eskcti.algashop.billing.domain.model.invoice;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aLineItemAlt;
import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.anInvoice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.DomainException;

class InvoiceTest {

  @Test
  void shouldIssueInvoice() {
    UUID customerId = UUID.randomUUID();
    Payer payer = aPayer();

    Invoice invoice = anInvoice()
        .orderId("order-1")
        .customerId(customerId)
        .payer(payer)
        .build();

    assertThat(invoice.getId()).isNotNull();
    assertThat(invoice.getOrderId()).isEqualTo("order-1");
    assertThat(invoice.getCustomerId()).isEqualTo(customerId);
    assertThat(invoice.getPayer()).isEqualTo(payer);
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("200.00");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getIssuedAt()).isNotNull();
    assertThat(invoice.getExpiresAt()).isAfter(invoice.getIssuedAt());
    assertThat(invoice.isUnpaid()).isTrue();
    assertThat(invoice.isPaid()).isFalse();
    assertThat(invoice.isCanceled()).isFalse();
  }

  @Test
  void shouldReturnUnmodifiableItems() {
    Invoice invoice = anInvoice().build();

    assertThatThrownBy(() -> invoice.getItems().add(aLineItemAlt()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldRejectBlankOrderIdOnIssue() {
    assertThatThrownBy(() -> anInvoice().orderId(" ").build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectEmptyItemsOnIssue() {
    assertThatThrownBy(() -> anInvoice().items(Collections.emptySet()).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullCustomerIdOnIssue() {
    assertThatThrownBy(() -> anInvoice().customerId(null).build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldMarkAsPaid() {
    Invoice invoice = anInvoice().build();

    invoice.markAsPaid();

    assertThat(invoice.isPaid()).isTrue();
    assertThat(invoice.getPaidAt()).isNotNull();
  }

  @Test
  void shouldNotMarkAsPaidWhenNotUnpaid() {
    Invoice invoice = anInvoice().status(InvoiceStatus.PAID).build();

    assertThatThrownBy(invoice::markAsPaid)
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be marked as paid");
  }

  @Test
  void shouldCancelInvoice() {
    Invoice invoice = anInvoice().build();

    invoice.cancel("customer request");

    assertThat(invoice.isCanceled()).isTrue();
    assertThat(invoice.getCancelReason()).isEqualTo("customer request");
    assertThat(invoice.getCanceledAt()).isNotNull();
  }

  @Test
  void shouldNotCancelAlreadyCanceledInvoice() {
    Invoice invoice = anInvoice()
        .status(InvoiceStatus.CANCELED)
        .cancelReason("first reason")
        .build();

    assertThatThrownBy(() -> invoice.cancel("second reason"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("already canceled");
  }

  @Test
  void shouldChangePaymentSettingsWithCreditCard() {
    Invoice invoice = anInvoice().build();
    UUID creditCardId = UUID.randomUUID();

    invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, creditCardId);

    assertThat(invoice.getPaymentSettings()).isNotNull();
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isEqualTo(creditCardId);
  }

  @Test
  void shouldChangePaymentSettingsWithGatewayBalance() {
    Invoice invoice = anInvoice().build();

    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    assertThat(invoice.getPaymentSettings()).isNotNull();
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isNull();
  }

  @Test
  void shouldRejectCreditCardPaymentWithoutCreditCardId() {
    Invoice invoice = anInvoice().build();

    assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldNotChangePaymentSettingsWhenNotUnpaid() {
    Invoice invoice = anInvoice().status(InvoiceStatus.PAID).build();

    assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldAssignPaymentGatewayCode() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();

    invoice.assignPaymentGatewayCode("gateway-123");

    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gateway-123");
  }

  @Test
  void shouldNotAssignPaymentGatewayCodeWhenNotUnpaid() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .status(InvoiceStatus.PAID)
        .build();

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("gateway-123"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldNotAssignBlankPaymentGatewayCode() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldNotReassignPaymentGatewayCode() {
    Invoice invoice = anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .gatewayCode("gateway-123")
        .build();

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("gateway-456"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Gateway code already assigned");
  }
}
