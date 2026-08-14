package com.eskcti.algashop.billing.application.creditcard.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreditCardQueryServiceImpl implements CreditCardQueryService {

    private final CreditCardRepository creditCardRepository;

    @Override
    @Transactional(readOnly = true)
    public CreditCardOutput findOne(UUID customerId, UUID creditCardId) {
        CreditCard creditCard = creditCardRepository.findByCustomerIdAndId(customerId, creditCardId)
                .orElseThrow(() -> new CreditCardNotFoundException());
        return mapToOutput(creditCard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditCardOutput> findByCustomer(UUID customerId) {
        return creditCardRepository.findAllByCustomerId(customerId)
                .stream()
                .map(this::mapToOutput)
                .toList();
    }

    private CreditCardOutput mapToOutput(CreditCard creditCard) {
        CreditCardOutput output = new CreditCardOutput();
        output.setId(creditCard.getId());
        output.setLastNumbers(creditCard.getLastNumbers());
        output.setExpMonth(creditCard.getExpMonth());
        output.setExpYear(creditCard.getExpYear());
        output.setBrand(creditCard.getBrand());
        return output;
    }
}
