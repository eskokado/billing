package com.eskcti.algashop.billing.presentation;

import com.eskcti.algashop.billing.AbstractControllerIT;
import com.eskcti.algashop.billing.application.invoice.query.InvoiceOutput;
import com.eskcti.algashop.billing.application.invoice.query.InvoiceQueryService;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.eskcti.algashop.billing.utils.MockJwtDecoderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyInvoiceController.class)
class MyInvoiceControllerIT extends AbstractControllerIT {

    @MockitoBean
    private InvoiceQueryService invoiceQueryService;

    @Test
    void shouldFindInvoiceOfAuthenticatedCustomer() throws Exception {
        String orderId = "order-1";
        UUID customerId = UUID.fromString(MockJwtDecoderFactory.DEFAULT_SUBJECT);
        UUID invoiceId = UUID.randomUUID();

        InvoiceOutput output = new InvoiceOutput();
        output.setId(invoiceId);
        output.setOrderId(orderId);
        output.setCustomerId(customerId);
        output.setTotalAmount(new BigDecimal("100.00"));
        output.setStatus(InvoiceStatus.PAID);
        output.setIssuedAt(OffsetDateTime.now());

        when(invoiceQueryService.findByOrderIdAndCustomerId(orderId, customerId)).thenReturn(output);

        authenticatedAsCustomer(get("/api/v1/customers/me/orders/{orderId}/invoice", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value(InvoiceStatus.PAID.name()));
    }

    @Test
    void shouldForbidNonCustomerFromReadingOwnInvoice() throws Exception {
        authenticated(get("/api/v1/customers/me/orders/{orderId}/invoice", "order-1"))
                .andExpect(status().isForbidden());
    }
}
