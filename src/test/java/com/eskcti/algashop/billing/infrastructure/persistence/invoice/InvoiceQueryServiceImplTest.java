package com.eskcti.algashop.billing.infrastructure.persistence.invoice;

import com.eskcti.algashop.billing.application.invoice.query.InvoiceOutput;
import com.eskcti.algashop.billing.application.utility.Mapper;
import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceQueryServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private Mapper mapper;

    @InjectMocks
    private InvoiceQueryServiceImpl service;

    @Test
    void shouldFindByOrderId() {
        String orderId = "order-abc";
        Invoice invoice = InvoiceTestDataBuilder.anInvoice().orderId(orderId).build();
        InvoiceOutput output = new InvoiceOutput();
        output.setOrderId(orderId);

        when(invoiceRepository.findByOrderId(orderId)).thenReturn(Optional.of(invoice));
        when(mapper.convert(invoice, InvoiceOutput.class)).thenReturn(output);

        InvoiceOutput result = service.findByOrderId(orderId);

        assertThat(result.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        String orderId = "not-found-order";
        when(invoiceRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByOrderId(orderId))
                .isInstanceOf(InvoiceNotFoundException.class);
    }
}
