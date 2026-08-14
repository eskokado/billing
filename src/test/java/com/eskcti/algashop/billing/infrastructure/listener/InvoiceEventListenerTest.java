package com.eskcti.algashop.billing.infrastructure.listener;

import com.eskcti.algashop.billing.domain.model.invoice.InvoiceCanceledEvent;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceIssuedEvent;
import com.eskcti.algashop.billing.domain.model.invoice.InvoicePaidEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InvoiceEventListenerTest {

    @InjectMocks
    private InvoiceEventListener listener;

    @Test
    void shouldListenIssuedEvent() {
        listener.listen(new InvoiceIssuedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "order-1", OffsetDateTime.now()));
    }

    @Test
    void shouldListenCanceledEvent() {
        listener.listen(new InvoiceCanceledEvent(
                UUID.randomUUID(), UUID.randomUUID(), "order-1", OffsetDateTime.now()));
    }

    @Test
    void shouldListenPaidEvent() {
        listener.listen(new InvoicePaidEvent(
                UUID.randomUUID(), UUID.randomUUID(), "order-1", OffsetDateTime.now()));
    }
}
