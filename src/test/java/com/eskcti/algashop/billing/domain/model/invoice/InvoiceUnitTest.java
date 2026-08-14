package com.eskcti.algashop.billing.domain.model.invoice;

import com.eskcti.algashop.billing.domain.model.DomainException;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceUnitTest {

    @Test
    void shouldIssueInvoiceWithUnpaidStatus() {
        Invoice invoice = Invoice.issue("order-abc", UUID.randomUUID(), aPayer(),
                oneItem());

        assertThat(invoice.isUnpaid()).isTrue();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldThrowWhenOrderIdIsBlank() {
        assertThatThrownBy(() -> Invoice.issue("", UUID.randomUUID(), aPayer(), oneItem()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenItemsIsEmpty() {
        assertThatThrownBy(() -> Invoice.issue("order-1", UUID.randomUUID(), aPayer(), Collections.emptySet()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMarkAsPaid() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.markAsPaid();

        assertThat(invoice.isPaid()).isTrue();
        assertThat(invoice.getPaidAt()).isNotNull();
    }

    @Test
    void shouldThrowWhenMarkingPaidInvoiceAsPaid() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.markAsPaid();

        assertThatThrownBy(invoice::markAsPaid)
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldCancelUnpaidInvoice() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.cancel("just because");

        assertThat(invoice.isCanceled()).isTrue();
        assertThat(invoice.getCancelReason()).isEqualTo("just because");
    }

    @Test
    void shouldThrowWhenCancelingCanceledInvoice() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.cancel("first");

        assertThatThrownBy(() -> invoice.cancel("second"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldAssignPaymentGatewayCodeWhenUnpaid() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, UUID.randomUUID());

        invoice.assignPaymentGatewayCode("pay-xpto");

        assertThat(invoice.getPaymentSettings().getGatewayCode()).isEqualTo("pay-xpto");
    }

    @Test
    void shouldThrowWhenAssigningGatewayCodeOnPaidInvoice() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, UUID.randomUUID());
        invoice.markAsPaid();

        assertThatThrownBy(() -> invoice.assignPaymentGatewayCode("pay-xpto"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldThrowWhenChangingPaymentSettingsOnPaidInvoice() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());
        invoice.markAsPaid();

        assertThatThrownBy(() -> invoice.changePaymentSettings(PaymentMethod.CREDIT_CARD, UUID.randomUUID()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldUpdatePaymentStatusPaid() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());

        invoice.updatePaymentStatus(PaymentStatus.PAID);

        assertThat(invoice.isPaid()).isTrue();
    }

    @Test
    void shouldUpdatePaymentStatusFailed() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());

        invoice.updatePaymentStatus(PaymentStatus.FAILED);

        assertThat(invoice.isCanceled()).isTrue();
        assertThat(invoice.getCancelReason()).isEqualTo("Payment failed");
    }

    @Test
    void shouldUpdatePaymentStatusRefunded() {
        Invoice invoice = Invoice.issue("order-1", UUID.randomUUID(), aPayer(), oneItem());

        invoice.updatePaymentStatus(PaymentStatus.REFUNDED);

        assertThat(invoice.isCanceled()).isTrue();
        assertThat(invoice.getCancelReason()).isEqualTo("Payment refunded");
    }

    private Set<LineItem> oneItem() {
        return Set.of(LineItem.builder()
                .number(1)
                .name("Item 1")
                .amount(new BigDecimal("100.00"))
                .build());
    }
}
