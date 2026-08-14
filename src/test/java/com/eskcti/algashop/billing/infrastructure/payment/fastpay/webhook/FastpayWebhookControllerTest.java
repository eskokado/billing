package com.eskcti.algashop.billing.infrastructure.payment.fastpay.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FastpayWebhookController.class)
class FastpayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FastpayWebhookHandler fastpayWebhookHandler;

    @Test
    void shouldReceiveValidWebhookEvent() throws Exception {
        FastpayPaymentWebhookEvent event = new FastpayPaymentWebhookEvent();
        event.setPaymentId("pay-abc");
        event.setReferenceCode("00000000-0000-0000-0000-000000000001");
        event.setStatus("PAID");
        event.setMethod("CREDIT");
        event.setNotifiedAt(OffsetDateTime.now());

        doNothing().when(fastpayWebhookHandler).process(any(FastpayPaymentWebhookEvent.class));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String body = mapper.writeValueAsString(event);

        mockMvc.perform(post("/api/v1/webhooks/fastpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenWebhookEventMissingFields() throws Exception {
        FastpayPaymentWebhookEvent event = new FastpayPaymentWebhookEvent();
        event.setPaymentId("");
        event.setReferenceCode("");
        event.setStatus("");
        event.setMethod("");
        event.setNotifiedAt(null);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String body = mapper.writeValueAsString(event);

        mockMvc.perform(post("/api/v1/webhooks/fastpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid fields"));
    }

    @Test
    void shouldReturn500WhenHandlerThrowsUnexpected() throws Exception {
        FastpayPaymentWebhookEvent event = new FastpayPaymentWebhookEvent();
        event.setPaymentId("pay-abc");
        event.setReferenceCode("00000000-0000-0000-0000-000000000001");
        event.setStatus("PAID");
        event.setMethod("CREDIT");
        event.setNotifiedAt(OffsetDateTime.now());

        doThrow(new RuntimeException("Handler broken"))
                .when(fastpayWebhookHandler).process(any(FastpayPaymentWebhookEvent.class));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String body = mapper.writeValueAsString(event);

        mockMvc.perform(post("/api/v1/webhooks/fastpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }
}
