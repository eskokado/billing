package com.eskcti.algashop.billing.domain.model;

public class DomainEntityNotFoundException extends DomainException {

    public DomainEntityNotFoundException() {
    }

    public DomainEntityNotFoundException(Throwable cause) {
        super(cause);
    }

    public DomainEntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainEntityNotFoundException(String message) {
        super(message);
    }
}
