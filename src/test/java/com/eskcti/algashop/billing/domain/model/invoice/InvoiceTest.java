package com.eskcti.algashop.billing.domain.model.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.DomainException;

class InvoiceTest {

  @Test
  void shouldIssueInvoice() {
    UUID customerId = UUID.randomUUID();
    Payer payer = PayerTest.validPayer();
    Set<LineItem> items = Set.of(LineItemTest.validLineItem());

    Invoice invoice = Invoice.issue("order-1", customerId, payer, items);

    assertThat(invoice.getId()).isNotNull();
    assertThat(invoice.getOrderId()).isEqualTo("order-1");
    assertThat(invoice.getCustomerId()).isEqualTo(customerId);
    assertThat(invoice.getPayer()).isEqualTo(payer);
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("100.00");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getIssuedAt()).isNotNull();
    assertThat(invoice.getExpiresAt()).isAfter(invoice.getIssuedAt());
    assertThat(invoice.isUnpaid()).isTrue();
    assertThat(invoice.isPaid()).isFalse();
    assertThat(invoice.isCanceled()).isFalse();
  }

  @Test
  void shouldReturnUnmodifiableItems() {
    Invoice invoice = issueValidInvoice();

    assertThatThrownBy(() -> invoice.getItems().add(LineItemTest.validLineItem()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldRejectBlankOrderIdOnIssue() {
    assertThatThrownBy(() -> Invoice.issue(" ", UUID.randomUUID(), PayerTest.validPayer(),
        Set.of(LineItemTest.validLineItem())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectEmptyItemsOnIssue() {
    assertThatThrownBy(() -> Invoice.issue("order-1", UUID.randomUUID(), PayerTest.validPayer(),
        Collections.emptySet()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullCustomerIdOnIssue() {
    assertThatThrownBy(() -> Invoice.issue("order-1", null, PayerTest.validPayer(),
        Set.of(LineItemTest.validLineItem())))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldMarkAsPaid() {
    Invoice invoice = issueValidInvoice();

    invoice.markAsPaid();

    assertThat(invoice.isPaid()).isTrue();
    assertThat(invoice.getPaidAt()).isNotNull();
  }

  @Test
  void shouldNotMarkAsPaidWhenNotUnpaid() {
    Invoice invoice = issueValidInvoice();
    invoice.markAsPaid();

    assertThatThrownBy(invoice::markAsPaid)
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be marked as paid");
  }

  @Test
  void shouldCancelInvoice() {
    Invoice invoice = issueValidInvoice();

    invoice.cancel("customer request");

    assertThat(invoice.isCanceled()).isTrue();
    assertThat(invoice.getCancelReason()).isEqualTo("customer request");
    assertThat(invoice.getCanceledAt()).isNotNull();
  }

  @Test
  void shouldNotCancelAlreadyCanceledInvoice() {
    Invoice invoice = issueValidInvoice();
    invoice.cancel("first reason");

    assertThatThrownBy(() -> invoice.cancel("second reason"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("already canceled");
  }

  @Test
  void shouldChangePaymentSettingsWithCreditCard() {
    Invoice invoice = issueValidInvoice();
    UUID creditCardId = UUID.randomUUID();

    invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, creditCardId);

    assertThat(invoice.getPaymentSettings()).isNotNull();
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isEqualTo(creditCardId);
  }

  @Test
  void shouldChangePaymentSettingsWithGatewayBalance() {
    Invoice invoice = issueValidInvoice();

    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    assertThat(invoice.getPaymentSettings()).isNotNull();
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isNull();
  }

  @Test
  void shouldRejectCreditCardPaymentWithoutCreditCardId() {
    Invoice invoice = issueValidInvoice();

    assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldNotChangePaymentSettingsWhenNotUnpaid() {
    Invoice invoice = issueValidInvoice();
    invoice.markAsPaid();

    assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldAssignPaymentGatewayCode() {
    Invoice invoice = issueValidInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    invoice.assignPaymentGatewayCode("gateway-123");

    assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("gateway-123");
  }

  @Test
  void shouldNotAssignPaymentGatewayCodeWhenNotUnpaid() {
    Invoice invoice = issueValidInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
    invoice.markAsPaid();

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("gateway-123"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("cannot be edited");
  }

  @Test
  void shouldNotAssignBlankPaymentGatewayCode() {
    Invoice invoice = issueValidInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldNotReassignPaymentGatewayCode() {
    Invoice invoice = issueValidInvoice();
    invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
    invoice.assignPaymentGatewayCode("gateway-123");

    assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("gateway-456"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Gateway code already assigned");
  }

  private Invoice issueValidInvoice() {
    return Invoice.issue(
        "order-1",
        UUID.randomUUID(),
        PayerTest.validPayer(),
        Set.of(LineItemTest.validLineItem()));
  }
}
