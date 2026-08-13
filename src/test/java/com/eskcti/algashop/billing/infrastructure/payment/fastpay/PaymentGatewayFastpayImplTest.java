package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;

class PaymentGatewayFastpayImplTest {

    private final PaymentGatewayFastpayImpl service = new PaymentGatewayFastpayImpl();

    @Test
    void shouldCapturePaymentReturningNull() {
        UUID invoiceId = UUID.randomUUID();
        PaymentRequest request = PaymentRequest.builder()
                .method(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("200.00"))
                .invoiceId(invoiceId)
                .creditCardId(UUID.randomUUID())
                .payer(aPayer())
                .build();

        Payment payment = service.capture(request);

        assertThat(payment).isNull();
    }

    @Test
    void shouldFindPaymentByCodeReturningNull() {
        Payment payment = service.findByCode("gateway-code");

        assertThat(payment).isNull();
    }
}
