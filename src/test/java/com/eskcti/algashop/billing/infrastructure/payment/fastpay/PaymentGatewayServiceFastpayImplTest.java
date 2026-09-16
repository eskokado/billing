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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import com.eskcti.algashop.billing.infrastructure.payment.AlgaShopPaymentPropreties;
import com.eskcti.algashop.billing.presentation.BadGatewayException;
import com.eskcti.algashop.billing.presentation.GatewayTimeoutException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentGatewayServiceFastpayImplTest {

        @Mock
        private ResilientFastpayPaymentClient resilientClient;

        @Mock
        private CreditCardRepository creditCardRepository;

        @Mock
        private AlgaShopPaymentPropreties algaShopPaymentPropreties;

        @Mock
        private AlgaShopPaymentPropreties.FastpayProperties fastpayProperties;

        @InjectMocks
        private PaymentGatewayServiceFastpayImpl service;

        private static final UUID DEFAULT_INVOICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final UUID DEFAULT_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
        private static final UUID DEFAULT_CREDIT_CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
        private static final String DEFAULT_GATEWAY_CODE = "gateway-abc-123";
        private static final String DEFAULT_GATEWAY_PAYMENT_ID = "pay-xyz-789";
        private static final String DEFAULT_HOSTNAME = "http://localhost:9999";
        private static final String DEFAULT_WEBHOOK_URL = "http://localhost:8082/payments/webhook";

        @BeforeEach
        void setUp() {
                when(algaShopPaymentPropreties.getFastpay()).thenReturn(fastpayProperties);
                when(fastpayProperties.getHostname()).thenReturn(DEFAULT_HOSTNAME);
                when(fastpayProperties.getWebhookUrl()).thenReturn(DEFAULT_WEBHOOK_URL);
        }

        private static FastpayPaymentModel aFastpayResponse(String id, String referenceCode, String method,
                        String status) {
                return FastpayPaymentModel.builder()
                                .id(id)
                                .referenceCode(referenceCode)
                                .method(method)
                                .status(status)
                                .build();
        }

        @Test
        void shouldCapturePaymentWithCreditCard() {
                CreditCard creditCard = CreditCard.brandNew(DEFAULT_CUSTOMER_ID, "1111", "Visa", 12, 2030,
                                DEFAULT_GATEWAY_CODE);
                creditCard.setGatewayCode(DEFAULT_GATEWAY_CODE);

                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                FastpayPaymentMethod.CREDIT.name(),
                                FastpayPaymentStatus.PAID.name());

                when(creditCardRepository.findById(DEFAULT_CREDIT_CARD_ID)).thenReturn(Optional.of(creditCard));
                when(resilientClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("100.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.CREDIT_CARD)
                                .creditCardId(DEFAULT_CREDIT_CARD_ID)
                                .payer(aPayer())
                                .build();

                Payment payment = service.capture(request);

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
                assertThat(payment.getInvoiceId()).isEqualTo(DEFAULT_INVOICE_ID);
                verify(resilientClient).capture(any(FastpayPaymentInput.class));
        }

        @Test
        void shouldCapturePaymentWithGatewayBalance() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                FastpayPaymentMethod.GATEWAY_BALANCE.name(),
                                FastpayPaymentStatus.PROCESSING.name());

                when(resilientClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("200.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                Payment payment = service.capture(request);

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
                assertThat(payment.getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
        }

        @Test
        void shouldThrowWhenCreditCardNotFoundOnCapture() {
                when(creditCardRepository.findById(DEFAULT_CREDIT_CARD_ID)).thenReturn(Optional.empty());

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("100.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.CREDIT_CARD)
                                .creditCardId(DEFAULT_CREDIT_CARD_ID)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(CreditCardNotFoundException.class);
        }

        @Test
        void shouldFindPaymentByCode() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                FastpayPaymentMethod.GATEWAY_BALANCE.name(),
                                FastpayPaymentStatus.PROCESSING.name());

                when(resilientClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

                Payment payment = service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID);

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
                assertThat(payment.getGatewayCode()).isEqualTo(DEFAULT_GATEWAY_PAYMENT_ID);
        }

        @Test
        void shouldPropagateGatewayTimeoutFromResilientClient() {
                when(resilientClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new GatewayTimeoutException("timeout", new RuntimeException()));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("100.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timeout");
        }

        @Test
        void shouldPropagateBadGatewayFromResilientClient() {
                when(resilientClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new BadGatewayException("unavailable", new RuntimeException()));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("100.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnUnknownPaymentMethod() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                "PIX",
                                FastpayPaymentStatus.PAID.name());

                when(resilientClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unexpected response");
        }

        @Test
        void shouldThrowBadGatewayOnUnknownPaymentStatus() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                FastpayPaymentMethod.GATEWAY_BALANCE.name(),
                                "EXPIRED");

                when(resilientClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unexpected response");
        }

        @Test
        void shouldPropagateExceptionsFromResilientClientDuringLookup() {
                when(resilientClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new GatewayTimeoutException("server error", new RuntimeException()));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldPropagateBadGatewayDuringLookup() {
                when(resilientClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new BadGatewayException("client error", new RuntimeException()));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }
}
