package com.eskcti.algashop.billing.presentation;

import com.eskcti.algashop.billing.application.invoice.management.GenerateInvoiceInput;
import com.eskcti.algashop.billing.application.invoice.management.InvoiceManagementApplicationService;
import com.eskcti.algashop.billing.application.invoice.management.LineItemInput;
import com.eskcti.algashop.billing.application.invoice.management.PayerData;
import com.eskcti.algashop.billing.application.invoice.management.PaymentSettingsInput;
import com.eskcti.algashop.billing.application.invoice.query.InvoiceOutput;
import com.eskcti.algashop.billing.application.invoice.query.InvoiceQueryService;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoiceQueryService invoiceQueryService;

    @MockBean
    private InvoiceManagementApplicationService invoiceManagementApplicationService;

    @Test
    void shouldGenerateInvoice() throws Exception {
        String orderId = "order-1";
        UUID invoiceId = UUID.randomUUID();
        GenerateInvoiceInput input = validInput(orderId);

        InvoiceOutput output = new InvoiceOutput();
        output.setId(invoiceId);
        output.setOrderId(orderId);
        output.setCustomerId(input.getCustomerId());
        output.setTotalAmount(new BigDecimal("100.00"));
        output.setStatus(InvoiceStatus.UNPAID);
        output.setIssuedAt(OffsetDateTime.now());

        when(invoiceManagementApplicationService.generate(any(GenerateInvoiceInput.class))).thenReturn(invoiceId);
        when(invoiceQueryService.findByOrderId(orderId)).thenReturn(output);

        mockMvc.perform(post("/api/v1/orders/{orderId}/invoice", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(InvoiceStatus.UNPAID.name()));
    }

    @Test
    void shouldGenerateInvoiceEvenWhenPaymentProcessingThrows() throws Exception {
        String orderId = "order-2";
        UUID invoiceId = UUID.randomUUID();
        GenerateInvoiceInput input = validInput(orderId);

        InvoiceOutput output = new InvoiceOutput();
        output.setId(invoiceId);
        output.setOrderId(orderId);
        output.setCustomerId(input.getCustomerId());
        output.setTotalAmount(new BigDecimal("100.00"));
        output.setStatus(InvoiceStatus.UNPAID);
        output.setIssuedAt(OffsetDateTime.now());

        when(invoiceManagementApplicationService.generate(any(GenerateInvoiceInput.class))).thenReturn(invoiceId);
        doThrow(new RuntimeException("Payment gateway down"))
                .when(invoiceManagementApplicationService).processPayment(invoiceId);
        when(invoiceQueryService.findByOrderId(orderId)).thenReturn(output);

        mockMvc.perform(post("/api/v1/orders/{orderId}/invoice", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldFindByOrder() throws Exception {
        String orderId = "order-3";
        InvoiceOutput output = new InvoiceOutput();
        output.setId(UUID.randomUUID());
        output.setOrderId(orderId);
        output.setCustomerId(UUID.randomUUID());
        output.setTotalAmount(new BigDecimal("50.00"));
        output.setStatus(InvoiceStatus.PAID);
        output.setIssuedAt(OffsetDateTime.now());
        output.setPaidAt(OffsetDateTime.now());

        when(invoiceQueryService.findByOrderId(orderId)).thenReturn(output);

        mockMvc.perform(get("/api/v1/orders/{orderId}/invoice", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(InvoiceStatus.PAID.name()));
    }

    @Test
    void shouldReturn400WhenFieldsAreMissing() throws Exception {
        String orderId = "order-empty";
        GenerateInvoiceInput input = new GenerateInvoiceInput();

        mockMvc.perform(post("/api/v1/orders/{orderId}/invoice", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid fields"))
                .andExpect(jsonPath("$.type").value("/errors/invalid-fields"))
                .andExpect(jsonPath("$.fields").isMap());
    }

    private GenerateInvoiceInput validInput(String orderId) {
        UUID customerId = UUID.randomUUID();
        GenerateInvoiceInput input = new GenerateInvoiceInput();
        input.setOrderId(orderId);
        input.setCustomerId(customerId);

        PaymentSettingsInput paymentSettings = new PaymentSettingsInput();
        paymentSettings.setMethod(PaymentMethod.GATEWAY_BALANCE);
        input.setPaymentSettings(paymentSettings);

        PayerData payer = new PayerData();
        payer.setFullName("John Doe");
        payer.setDocument("111.222.333-44");
        payer.setEmail("john@email.com");
        payer.setPhone("11-99999-8888");
        com.eskcti.algashop.billing.application.invoice.management.AddressData address = new com.eskcti.algashop.billing.application.invoice.management.AddressData();
        address.setStreet("Street");
        address.setNumber("123");
        address.setNeighborhood("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("12345-678");
        payer.setAddress(address);
        input.setPayer(payer);

        LineItemInput line = new LineItemInput();
        line.setName("Item 1");
        line.setAmount(new BigDecimal("100.00"));
        input.setItems(Set.of(line));

        return input;
    }
}
