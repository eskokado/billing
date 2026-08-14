package com.eskcti.algashop.billing.infrastructure.creditcard.fastpay;

import com.eskcti.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardProviderServiceFastpayImplTest {

    @Mock
    private FastpayCreditCardAPIClient fastpayCreditCardAPIClient;

    @InjectMocks
    private CreditCardProviderServiceFastpayImpl service;

    private static FastpayCreditCardResponse aResponse(String id) {
        FastpayCreditCardResponse r = new FastpayCreditCardResponse();
        r.setId(id);
        r.setLastNumbers("1111");
        r.setBrand("Visa");
        r.setExpMonth(12);
        r.setExpYear(2030);
        return r;
    }

    @Test
    void shouldRegisterTokenizedCreditCard() {
        UUID customerId = UUID.randomUUID();
        String tokenized = "tok_abc";
        FastpayCreditCardResponse response = aResponse("card-abc-123");
        when(fastpayCreditCardAPIClient.create(org.mockito.ArgumentMatchers.any(FastpayCreditCardInput.class)))
                .thenReturn(response);

        LimitedCreditCard card = service.register(customerId, tokenized);

        assertThat(card.getGatewayCode()).isEqualTo("card-abc-123");
        assertThat(card.getLastNumbers()).isEqualTo("1111");
        assertThat(card.getBrand()).isEqualTo("Visa");
        assertThat(card.getExpMonth()).isEqualTo(12);
        assertThat(card.getExpYear()).isEqualTo(2030);
    }

    @Test
    void shouldFindById() {
        String gatewayCode = "card-def-456";
        FastpayCreditCardResponse response = aResponse(gatewayCode);
        when(fastpayCreditCardAPIClient.findById(gatewayCode)).thenReturn(response);

        Optional<LimitedCreditCard> result = service.findById(gatewayCode);

        assertThat(result).isPresent();
        assertThat(result.get().getGatewayCode()).isEqualTo(gatewayCode);
    }

    @Test
    void shouldReturnEmptyWhenIdNotFoundOnClient() {
        String gatewayCode = "card-not-found";
        when(fastpayCreditCardAPIClient.findById(gatewayCode))
                .thenThrow(
                        HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        Optional<LimitedCreditCard> result = service.findById(gatewayCode);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteGatewayCard() {
        String gatewayCode = "card-ghi-789";
        doNothing().when(fastpayCreditCardAPIClient).delete(gatewayCode);

        service.delete(gatewayCode);

        verify(fastpayCreditCardAPIClient).delete(gatewayCode);
    }
}
