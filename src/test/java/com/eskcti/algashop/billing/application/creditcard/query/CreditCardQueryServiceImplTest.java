package com.eskcti.algashop.billing.application.creditcard.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;

@ExtendWith(MockitoExtension.class)
class CreditCardQueryServiceImplTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @InjectMocks
    private CreditCardQueryServiceImpl service;

    private static final UUID DEFAULT_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_CREDIT_CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SECOND_CREDIT_CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void shouldFindOneCreditCardByCustomerAndId() {
        CreditCard card = CreditCard.brandNew(DEFAULT_CUSTOMER_ID, "1111", "Visa", 12, 2030, "gateway-a");
        card.setGatewayCode("gateway-a");
        when(creditCardRepository.findByCustomerIdAndId(DEFAULT_CUSTOMER_ID, DEFAULT_CREDIT_CARD_ID))
                .thenReturn(Optional.of(card));

        CreditCardOutput output = service.findOne(DEFAULT_CUSTOMER_ID, DEFAULT_CREDIT_CARD_ID);

        assertThat(output.getLastNumbers()).isEqualTo("1111");
        assertThat(output.getBrand()).isEqualTo("Visa");
        assertThat(output.getExpMonth()).isEqualTo(12);
        assertThat(output.getExpYear()).isEqualTo(2030);
    }

    @Test
    void shouldThrowWhenFindOneCardNotFound() {
        when(creditCardRepository.findByCustomerIdAndId(DEFAULT_CUSTOMER_ID, DEFAULT_CREDIT_CARD_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOne(DEFAULT_CUSTOMER_ID, DEFAULT_CREDIT_CARD_ID))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void shouldFindAllCardsByCustomer() {
        CreditCard card1 = CreditCard.brandNew(DEFAULT_CUSTOMER_ID, "1111", "Visa", 12, 2030, "gateway-a");
        card1.setGatewayCode("gateway-a");
        CreditCard card2 = CreditCard.brandNew(DEFAULT_CUSTOMER_ID, "9999", "Mastercard", 6, 2029, "gateway-b");
        card2.setGatewayCode("gateway-b");

        when(creditCardRepository.findAllByCustomerId(DEFAULT_CUSTOMER_ID)).thenReturn(List.of(card1, card2));

        List<CreditCardOutput> list = service.findByCustomer(DEFAULT_CUSTOMER_ID);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getLastNumbers()).isEqualTo("1111");
        assertThat(list.get(1).getLastNumbers()).isEqualTo("9999");
        assertThat(list.get(1).getBrand()).isEqualTo("Mastercard");
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoCards() {
        when(creditCardRepository.findAllByCustomerId(DEFAULT_CUSTOMER_ID)).thenReturn(List.of());

        List<CreditCardOutput> list = service.findByCustomer(DEFAULT_CUSTOMER_ID);

        assertThat(list).isEmpty();
    }
}
