package com.eskcti.algashop.billing.presentation;

import com.eskcti.algashop.billing.application.creditcard.management.CreditCardManagementService;
import com.eskcti.algashop.billing.application.creditcard.management.TokenizedCreditCardInput;
import com.eskcti.algashop.billing.application.creditcard.query.CreditCardOutput;
import com.eskcti.algashop.billing.application.creditcard.query.CreditCardQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreditCardController.class)
class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreditCardManagementService creditCardManagementService;

    @MockBean
    private CreditCardQueryService creditCardQueryService;

    @Test
    void shouldRegisterCreditCard() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();

        TokenizedCreditCardInput input = new TokenizedCreditCardInput();
        input.setTokenizedCard("tok_abc");

        CreditCardOutput output = new CreditCardOutput();
        output.setId(creditCardId);
        output.setLastNumbers("1111");
        output.setBrand("Visa");
        output.setExpMonth(12);
        output.setExpYear(2030);

        when(creditCardManagementService.register(any(TokenizedCreditCardInput.class))).thenReturn(creditCardId);
        when(creditCardQueryService.findOne(customerId, creditCardId)).thenReturn(output);

        mockMvc.perform(post("/api/v1/customers/{customerId}/credit-cards", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(creditCardId.toString()))
                .andExpect(jsonPath("$.lastNumbers").value("1111"))
                .andExpect(jsonPath("$.brand").value("Visa"));
    }

    @Test
    void shouldReturn400WhenTokenizedCardIsBlank() throws Exception {
        UUID customerId = UUID.randomUUID();
        TokenizedCreditCardInput input = new TokenizedCreditCardInput();
        input.setTokenizedCard("   ");

        mockMvc.perform(post("/api/v1/customers/{customerId}/credit-cards", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid fields"));
    }

    @Test
    void shouldReturnAllCreditCardsOfCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreditCardOutput card1 = new CreditCardOutput();
        card1.setId(UUID.randomUUID());
        card1.setLastNumbers("1111");
        card1.setBrand("Visa");
        card1.setExpMonth(12);
        card1.setExpYear(2030);
        CreditCardOutput card2 = new CreditCardOutput();
        card2.setId(UUID.randomUUID());
        card2.setLastNumbers("9999");
        card2.setBrand("Mastercard");
        card2.setExpMonth(6);
        card2.setExpYear(2028);

        when(creditCardQueryService.findByCustomer(customerId)).thenReturn(List.of(card1, card2));

        mockMvc.perform(get("/api/v1/customers/{customerId}/credit-cards", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lastNumbers").value("1111"))
                .andExpect(jsonPath("$[1].lastNumbers").value("9999"));
    }

    @Test
    void shouldFindOneCreditCard() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();
        CreditCardOutput card = new CreditCardOutput();
        card.setId(creditCardId);
        card.setLastNumbers("1111");
        card.setBrand("Visa");
        card.setExpMonth(12);
        card.setExpYear(2030);

        when(creditCardQueryService.findOne(customerId, creditCardId)).thenReturn(card);

        mockMvc.perform(get("/api/v1/customers/{customerId}/credit-cards/{creditCardId}",
                        customerId, creditCardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creditCardId.toString()))
                .andExpect(jsonPath("$.lastNumbers").value("1111"));
    }

    @Test
    void shouldDeleteCreditCard() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/customers/{customerId}/credit-cards/{creditCardId}",
                        customerId, creditCardId))
                .andExpect(status().isNoContent());
    }
}
