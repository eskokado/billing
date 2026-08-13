package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceFastpayImplTest {

    @Mock
    private FastpayPaymentAPIClient fastpayPaymentAPIClient;

    @Mock
    private CreditCardRepository creditCardRepository;

    @InjectMocks
    private PaymentGatewayServiceFastpayImpl service;

    private static final UUID DEFAULT_INVOICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DEFAULT_CREDIT_CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String DEFAULT_GATEWAY_CODE = "gateway-abc-123";
    private static final String DEFAULT_GATEWAY_PAYMENT_ID = "pay-xyz-789";

    @Test
    void shouldCapturePaymentWithCreditCard() {
        PaymentRequest request = PaymentRequest.builder()
                .method(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("1000.00"))
                .invoiceId(DEFAULT_INVOICE_ID)
                .creditCardId(DEFAULT_CREDIT_CARD_ID)
                .payer(aPayer())
                .build();

        CreditCard creditCard = CreditCard.brandNew(
                DEFAULT_CUSTOMER_ID, "1234", "Visa", 12, 2030, DEFAULT_GATEWAY_CODE);

        FastpayPaymentModel response = buildResponseModel(
                DEFAULT_GATEWAY_PAYMENT_ID, DEFAULT_INVOICE_ID.toString(),
                FastpayPaymentMethod.CREDIT.name(), FastpayPaymentStatus.PAID.name());

        when(creditCardRepository.findById(DEFAULT_CREDIT_CARD_ID)).thenReturn(Optional.of(creditCard));
        when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

        Payment payment = service.capture(request);

        assertThat(payment.getGatewayCode()).isEqualTo(DEFAULT_GATEWAY_PAYMENT_ID);
        assertThat(payment.getInvoiceId()).isEqualTo(DEFAULT_INVOICE_ID);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(creditCardRepository).findById(DEFAULT_CREDIT_CARD_ID);
    }

    @Test
    void shouldCapturePaymentWithGatewayBalance() {
        PaymentRequest request = PaymentRequest.builder()
                .method(PaymentMethod.GATEWAY_BALANCE)
                .amount(new BigDecimal("500.00"))
                .invoiceId(DEFAULT_INVOICE_ID)
                .payer(aPayer())
                .build();

        FastpayPaymentModel response = buildResponseModel(
                DEFAULT_GATEWAY_PAYMENT_ID, DEFAULT_INVOICE_ID.toString(),
                FastpayPaymentMethod.GATEWAY_BALANCE.name(), FastpayPaymentStatus.PENDING.name());

        when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

        Payment payment = service.capture(request);

        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldThrowWhenCreditCardNotFoundOnCapture() {
        PaymentRequest request = PaymentRequest.builder()
                .method(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("100.00"))
                .invoiceId(DEFAULT_INVOICE_ID)
                .creditCardId(DEFAULT_CREDIT_CARD_ID)
                .payer(aPayer())
                .build();

        when(creditCardRepository.findById(DEFAULT_CREDIT_CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.capture(request))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void shouldFindPaymentByCode() {
        FastpayPaymentModel response = buildResponseModel(
                DEFAULT_GATEWAY_PAYMENT_ID, DEFAULT_INVOICE_ID.toString(),
                FastpayPaymentMethod.CREDIT.name(), FastpayPaymentStatus.PROCESSING.name());

        when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

        Payment payment = service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID);

        assertThat(payment.getGatewayCode()).isEqualTo(DEFAULT_GATEWAY_PAYMENT_ID);
        assertThat(payment.getInvoiceId()).isEqualTo(DEFAULT_INVOICE_ID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void shouldThrowWhenConvertingUnknownPaymentMethod() {
        FastpayPaymentModel response = buildResponseModel(
                DEFAULT_GATEWAY_PAYMENT_ID, DEFAULT_INVOICE_ID.toString(),
                "PIX", FastpayPaymentStatus.PAID.name());

        when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

        assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown payment method: PIX");
    }

    @Test
    void shouldThrowWhenConvertingUnknownPaymentStatus() {
        FastpayPaymentModel response = buildResponseModel(
                DEFAULT_GATEWAY_PAYMENT_ID, DEFAULT_INVOICE_ID.toString(),
                FastpayPaymentMethod.CREDIT.name(), "EXPIRED");

        when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

        assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown payment status: EXPIRED");
    }

    private FastpayPaymentModel buildResponseModel(String id, String referenceCode,
                                                   String method, String status) {
        FastpayPaymentModel model = new FastpayPaymentModel();
        model.setId(id);
        model.setReferenceCode(referenceCode);
        model.setMethod(method);
        model.setStatus(status);
        model.setTotalAmount(new BigDecimal("1000.00"));
        return model;
    }
}
