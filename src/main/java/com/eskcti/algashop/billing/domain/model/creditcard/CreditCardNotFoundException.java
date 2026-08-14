package com.eskcti.algashop.billing.domain.model.creditcard;

import com.eskcti.algashop.billing.domain.model.DomainEntityNotFoundException;

public class CreditCardNotFoundException extends DomainEntityNotFoundException {

    public CreditCardNotFoundException() {
    }

    public CreditCardNotFoundException(String message) {
        super(message);
    }
}
