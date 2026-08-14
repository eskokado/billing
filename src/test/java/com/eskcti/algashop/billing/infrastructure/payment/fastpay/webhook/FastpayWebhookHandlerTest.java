package com.eskcti.algashop.billing.infrastructure.payment.fastpay.webhook;

import com.eskcti.algashop.billing.application.invoice.management.InvoiceManagementApplicationService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import com.eskcti.algashop.billing.infrastructure.payment.fastpay.FastpayPaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FastpayWebhookHandlerTest {

    @Mock
    private InvoiceManagementApplicationService invoiceManagementApplicationService;

    @InjectMocks
    private FastpayWebhookHandler handler;

    @Test
    void shouldUpdatePaymentStatusWhenEventValid() {
        UUID invoiceId = UUID.randomUUID();
        FastpayPaymentWebhookEvent event = new FastpayPaymentWebhookEvent();
        event.setPaymentId("pay-abc");
        event.setReferenceCode(invoiceId.toString());
        event.setStatus(FastpayPaymentStatus.PAID.name());
        event.setMethod("CREDIT");
        event.setNotifiedAt(OffsetDateTime.now());

        handler.process(event);

        verify(invoiceManagementApplicationService).updatePaymentStatus(
                eq(invoiceId),
                eq(PaymentStatus.PAID));
    }

    @Test
    void shouldThrowWhenReferenceCodeIsNotAUuid() {
        FastpayPaymentWebhookEvent event = new FastpayPaymentWebhookEvent();
        event.setPaymentId("pay-abc");
        event.setReferenceCode("order-not-uuid");
        event.setStatus(FastpayPaymentStatus.PROCESSING.name());
        event.setMethod("GATEWAY_BALANCE");
        event.setNotifiedAt(OffsetDateTime.now());

        assertThatThrownBy(() -> handler.process(event))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenEventStatusIsUnknown() {
        UUID invoiceId = UUID.randomUUID();
        FastpayPaymentWebhookEvent event = new FastpayPaymentWebhookEvent();
        event.setPaymentId("pay-abc");
        event.setReferenceCode(invoiceId.toString());
        event.setStatus("NOT_VALID_STATUS");
        event.setMethod("CREDIT");
        event.setNotifiedAt(OffsetDateTime.now());

        assertThatThrownBy(() -> handler.process(event))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
