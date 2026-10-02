package com.eskcti.algashop.billing.infrastructure.creditcard.fastpay;

import com.eskcti.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import com.eskcti.algashop.billing.infrastructure.AbstractFastpayIT;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;

class CreditCardProviderServiceFastpayImplIT extends AbstractFastpayIT {

    @Test
    public void shouldRegisterCreditCard() {
        LimitedCreditCard limitedCreditCard = registerCard();
        Assertions.assertThat(limitedCreditCard.getGatewayCode()).isNotBlank();
    }

    @Test
    public void shouldFindRegisteredCreditCard() {
        LimitedCreditCard limitedCreditCard = registerCard();

        LimitedCreditCard limitedCreditCardFound = creditCardProvider.findById(limitedCreditCard.getGatewayCode())
                .orElseThrow();

        Assertions.assertThat(limitedCreditCardFound.getGatewayCode()).isEqualTo(limitedCreditCard.getGatewayCode());
    }

    @Test
    public void shouldDeleteRegisteredCreditCard() {
        LimitedCreditCard limitedCreditCard = registerCard();
        creditCardProvider.delete(limitedCreditCard.getGatewayCode());
    }

}