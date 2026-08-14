package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
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
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

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
        private FastpayPaymentAPIClient fastpayPaymentAPIClient;

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
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

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
                verify(fastpayPaymentAPIClient).capture(any(FastpayPaymentInput.class));
        }

        @Test
        void shouldCapturePaymentWithGatewayBalance() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                FastpayPaymentMethod.GATEWAY_BALANCE.name(),
                                FastpayPaymentStatus.PROCESSING.name());

                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

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

                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

                Payment payment = service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID);

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
                assertThat(payment.getGatewayCode()).isEqualTo(DEFAULT_GATEWAY_PAYMENT_ID);
        }

        @Test
        void shouldThrowWhenConvertingUnknownPaymentMethod() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                "PIX",
                                FastpayPaymentStatus.PAID.name());

                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unexpected response");
        }

        @Test
        void shouldThrowWhenConvertingUnknownPaymentStatus() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                FastpayPaymentMethod.GATEWAY_BALANCE.name(),
                                "EXPIRED");

                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID)).thenReturn(response);

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unexpected response");
        }

        @Test
        void shouldThrowGatewayTimeoutOnSocketTimeoutExceptionDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("I/O error",
                                                new SocketTimeoutException("connect timed out")));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("100.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnQueryTimeoutException() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new QueryTimeoutException("database query timeout: timed out",
                                                new RuntimeException()));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessExceptionWithoutTimeout() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("Connection refused",
                                                new java.net.ConnectException("Connection refused")));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("50.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnHttpClientErrorDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("50.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnHttpServerErrorDuringLookup() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Internal Server Error"));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnErrorResponseException4xxDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("50.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnErrorResponseException5xx() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(HttpStatus.BAD_GATEWAY));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnUnexpectedException() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new RuntimeException("Unexpected something"));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("Unexpected error");
        }

        @Test
        void shouldThrowBadGatewayOnHttpClientErrorDuringLookup() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldRethrowHttpClientNotFoundOnCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found",
                                                null, null, null));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("50.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        void shouldThrowBadGatewayOnCaptureWhenErrorResponseHasUnknownStatusCode() {
                HttpStatusCode weird = HttpStatusCode.valueOf(999);
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(weird));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnQueryTimeoutWithWordTimeoutOnly() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new QueryTimeoutException("the operation timeout after many seconds",
                                                new RuntimeException()));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnQueryTimeoutWithHyphenWord() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new QueryTimeoutException("gateway time-out exceeded",
                                                new RuntimeException()));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessWithNullMessageOnCapture() {
                java.net.ConnectException cause = new java.net.ConnectException("Connection refused");
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException(null, cause));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowGatewayTimeoutOnSocketTimeoutDuringLookup() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("I/O error",
                                                new SocketTimeoutException("Read timed out")));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnQueryTimeoutWithTimeoutWordDuringLookup() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new QueryTimeoutException("restclient timeout expired",
                                                new RuntimeException()));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessWithNullMessageOnLookup() {
                java.net.ConnectException cause = new java.net.ConnectException("Refused");
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException(null, cause));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnErrorResponseExceptionWithUnknownStatusCodeDuringLookup() {
                HttpStatusCode weird = HttpStatusCode.valueOf(499);
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(weird));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnHttpServerErrorDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Internal Server Error"));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("50.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnErrorResponseException5xxDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(HttpStatus.BAD_GATEWAY));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnIllegalArgumentExceptionDuringCapture() {
                FastpayPaymentModel response = aFastpayResponse(
                                DEFAULT_GATEWAY_PAYMENT_ID,
                                DEFAULT_INVOICE_ID.toString(),
                                "UNKNOWN_METHOD",
                                FastpayPaymentStatus.PAID.name());

                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unexpected response");
        }

        @Test
        void shouldThrowBadGatewayOnUnexpectedExceptionDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new RuntimeException("kaboom"));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("Unexpected error");
        }

        @Test
        void shouldRethrowHttpClientNotFoundOnLookup() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND,
                                                "Not Found", null, null, null));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        void shouldThrowBadGatewayOnCaptureWhenResourceAccessHasMessageWithoutTimeoutWords() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("Connection reset by peer",
                                                new ConnectException("Connection refused")));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("Payment gateway is unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnLookupWhenResourceAccessHasMessageWithoutTimeoutWords() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("Connection reset by peer",
                                                new ConnectException("Connection refused")));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("Payment gateway is unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnLookupWhenErrorResponseHasUnknownStatusCode() {
                HttpStatusCode weird = HttpStatusCode.valueOf(999);
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(weird));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnCaptureWhenResourceAccessMessageHasTimedOutWord() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("Read timed out while writing",
                                                new ConnectException()));

                PaymentRequest request = PaymentRequest.builder()
                                .amount(new BigDecimal("10.00"))
                                .invoiceId(DEFAULT_INVOICE_ID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .payer(aPayer())
                                .build();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnLookupWhenResourceAccessMessageHasTimedOutWord() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("Read timed out while reading",
                                                new ConnectException()));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnLookupWhenResourceAccessMessageHasTimeOutHyphen() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("restclient time-out expired",
                                                new ConnectException()));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnLookupWhenErrorResponseHasKnown4xxStatusCode() {
                HttpStatus badRequest = HttpStatus.BAD_REQUEST;
                when(fastpayPaymentAPIClient.findById(DEFAULT_GATEWAY_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(badRequest));

                assertThatThrownBy(() -> service.findByCode(DEFAULT_GATEWAY_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }
}
