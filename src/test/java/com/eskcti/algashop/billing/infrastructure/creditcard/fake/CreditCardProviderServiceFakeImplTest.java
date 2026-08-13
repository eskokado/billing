package com.eskcti.algashop.billing.infrastructure.creditcard.fake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.creditcard.LimitedCreditCard;

class CreditCardProviderServiceFakeImplTest {

    private final CreditCardProviderServiceFakeImpl service = new CreditCardProviderServiceFakeImpl();

    @Test
    void shouldRegisterFakeCreditCard() {
        UUID customerId = UUID.randomUUID();
        LimitedCreditCard card = service.register(customerId, "tokenized-card");

        assertThat(card).isNotNull();
        assertThat(card.getBrand()).isEqualTo("Visa");
        assertThat(card.getExpMonth()).isEqualTo(1);
        assertThat(card.getExpYear()).isEqualTo(Year.now().getValue() + 5);
        assertThat(card.getLastNumbers()).isEqualTo("1234");
        assertThat(card.getGatewayCode()).isNotBlank();
    }

    @Test
    void shouldFindFakeCreditCardById() {
        Optional<LimitedCreditCard> possibleCard = service.findById("any-gateway-code");

        assertThat(possibleCard).isPresent();
        LimitedCreditCard card = possibleCard.get();
        assertThat(card.getBrand()).isEqualTo("Visa");
        assertThat(card.getExpMonth()).isEqualTo(1);
        assertThat(card.getLastNumbers()).isEqualTo("1234");
    }

    @Test
    void shouldDeleteWithoutThrowing() {
        assertThatCode(() -> service.delete("any-gateway-code"))
                .doesNotThrowAnyException();
    }
}
