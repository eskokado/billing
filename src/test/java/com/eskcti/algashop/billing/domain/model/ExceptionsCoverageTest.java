package com.eskcti.algashop.billing.domain.model;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.presentation.BadGatewayException;
import com.eskcti.algashop.billing.presentation.GatewayTimeoutException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionsCoverageTest {

    @Test
    void shouldInstantiateAllDomainExceptionConstructors() {
        RuntimeException cause = new RuntimeException("cause");

        DomainException empty = new DomainException();
        DomainException msg = new DomainException("msg");
        DomainException msgCause = new DomainException("msg", cause);
        DomainException causeOnly = new DomainException(cause);
        DomainException full = new DomainException("msg", cause, true, true);

        assertThat(empty).hasMessage(null);
        assertThat(msg).hasMessage("msg");
        assertThat(msgCause).hasMessage("msg").hasCause(cause);
        assertThat(causeOnly).hasCause(cause);
        assertThat(full).hasMessage("msg").hasCause(cause);
    }

    @Test
    void shouldInstantiateAllDomainEntityNotFoundExceptionConstructors() {
        RuntimeException cause = new RuntimeException("cause");

        DomainEntityNotFoundException empty = new DomainEntityNotFoundException();
        DomainEntityNotFoundException msg = new DomainEntityNotFoundException("msg");
        DomainEntityNotFoundException msgCause = new DomainEntityNotFoundException("msg", cause);
        DomainEntityNotFoundException causeOnly = new DomainEntityNotFoundException(cause);

        assertThat(empty).hasMessage(null);
        assertThat(msg).hasMessage("msg");
        assertThat(msgCause).hasMessage("msg").hasCause(cause);
        assertThat(causeOnly).hasCause(cause);
    }

    @Test
    void shouldInstantiateGatewayAndBadGatewayEmptyConstructors() {
        RuntimeException cause = new RuntimeException("cause");

        BadGatewayException bgEmpty = new BadGatewayException();
        BadGatewayException bgCause = new BadGatewayException("msg", cause);
        GatewayTimeoutException gtEmpty = new GatewayTimeoutException();
        GatewayTimeoutException gtCause = new GatewayTimeoutException("msg", cause);

        assertThat(bgEmpty).hasMessage(null);
        assertThat(bgCause).hasMessage("msg").hasCause(cause);
        assertThat(gtEmpty).hasMessage(null);
        assertThat(gtCause).hasMessage("msg").hasCause(cause);

        assertThatThrownBy(() -> {
            throw bgEmpty;
        }).isInstanceOf(BadGatewayException.class);
        assertThatThrownBy(() -> {
            throw gtEmpty;
        }).isInstanceOf(GatewayTimeoutException.class);
    }

    @Test
    void shouldInstantiateInvoiceAndCreditCardNotFound() {
        InvoiceNotFoundException inv = new InvoiceNotFoundException();
        CreditCardNotFoundException ccEmpty = new CreditCardNotFoundException();
        CreditCardNotFoundException ccMsg = new CreditCardNotFoundException("not found");

        assertThat(inv).isInstanceOf(DomainEntityNotFoundException.class);
        assertThat(ccEmpty).isInstanceOf(DomainEntityNotFoundException.class);
        assertThat(ccMsg).hasMessage("not found");
    }
}
