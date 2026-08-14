package com.eskcti.algashop.billing.application.creditcard.management;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardProviderService;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardManagementServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private CreditCardProviderService creditCardProviderService;

    @InjectMocks
    private CreditCardManagementService service;

    @Test
    void shouldRegisterCreditCard() {
        UUID customerId = UUID.randomUUID();
        String tokenizedCard = "tok_123";
        TokenizedCreditCardInput input = new TokenizedCreditCardInput();
        input.setCustomerId(customerId);
        input.setTokenizedCard(tokenizedCard);

        LimitedCreditCard limited = LimitedCreditCard.builder()
                .gatewayCode("card_abc")
                .lastNumbers("1111")
                .brand("Visa")
                .expMonth(12)
                .expYear(2030)
                .build();
        when(creditCardProviderService.register(customerId, tokenizedCard)).thenReturn(limited);
        when(creditCardRepository.saveAndFlush(any(CreditCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID result = service.register(input);

        assertThat(result).isNotNull();
        verify(creditCardProviderService).register(customerId, tokenizedCard);
        verify(creditCardRepository).saveAndFlush(any(CreditCard.class));
    }

    @Test
    void shouldDeleteCreditCard() {
        UUID customerId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();
        CreditCard card = CreditCard.brandNew(customerId, "1111", "Visa", 12, 2030, "gateway-code-xyz");
        ReflectionTestUtils.setField(card, "id", creditCardId);

        when(creditCardRepository.findByCustomerIdAndId(customerId, creditCardId)).thenReturn(Optional.of(card));

        service.delete(customerId, creditCardId);

        verify(creditCardRepository).delete(card);
        verify(creditCardProviderService).delete("gateway-code-xyz");
    }

    @Test
    void shouldThrowWhenDeletingCreditCardOfAnotherCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();

        when(creditCardRepository.findByCustomerIdAndId(customerId, creditCardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(customerId, creditCardId))
                .isInstanceOf(CreditCardNotFoundException.class);
    }
}
