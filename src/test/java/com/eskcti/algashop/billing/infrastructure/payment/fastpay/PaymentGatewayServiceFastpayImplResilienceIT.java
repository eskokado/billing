package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.infrastructure.AbstractFastpayIT;
import com.eskcti.algashop.billing.presentation.BadGatewayException;
import com.eskcti.algashop.billing.presentation.GatewayTimeoutException;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentGatewayServiceFastpayImplResilienceIT extends AbstractFastpayIT {

        @Autowired
        private PaymentGatewayService service;

        @BeforeAll
        public static void beforeAll() {
                startMock();
        }

        @AfterAll
        public static void afterAll() {
                stopMock();
        }

        @BeforeEach
        void setUp() {
                WireMock.configureFor("localhost", 8788);
        }

        @AfterEach
        void tearDown() {
                WireMock.reset();
        }

        @Test
        void shouldThrowBadGatewayOnCapture4xxClientError() {
                WireMock.stubFor(post(urlMatching("/api/v1/payments"))
                                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"error\":\"invalid\"}")));

                PaymentRequest request = aGatewayBalanceRequest();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("capturing");
        }

        @Test
        void shouldThrowGatewayTimeoutOnCapture5xxServerError() {
                WireMock.stubFor(post(urlMatching("/api/v1/payments"))
                                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"error\":\"boom\"}")));

                PaymentRequest request = aGatewayBalanceRequest();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("capturing");
        }

        @Test
        void shouldThrowBadGatewayOnLookup4xxClientError() {
                String gatewayCode = "pay_xxx_client_error";
                WireMock.stubFor(get(urlMatching("/api/v1/payments/" + gatewayCode))
                                .willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"error\":\"unauthorized\"}")));

                assertThatThrownBy(() -> service.findByCode(gatewayCode))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("looking up");
        }

        @Test
        void shouldThrowGatewayTimeoutOnLookup5xxServerError() {
                String gatewayCode = "pay_xxx_server_error";
                WireMock.stubFor(get(urlMatching("/api/v1/payments/" + gatewayCode))
                                .willReturn(aResponse().withStatus(HttpStatus.BAD_GATEWAY.value())
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"error\":\"upstream\"}")));

                assertThatThrownBy(() -> service.findByCode(gatewayCode))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("looking up");
        }

        @Test
        void shouldThrowBadGatewayOnCaptureUnexpectedException() {
                WireMock.stubFor(post(urlMatching("/api/v1/payments"))
                                .willReturn(aResponse().withStatus(201)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("not a json")));

                PaymentRequest request = aGatewayBalanceRequest();

                assertThatThrownBy(() -> service.capture(request))
                                .isInstanceOf(BadGatewayException.class);
        }

        @Test
        void shouldCaptureWithSuccess() {
                UUID invoiceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
                String validBody = String.format("""
                                {"id":"pay_abc","status":"PAID","createdAt":"2025-10-07T15:00:00Z",
                                 "paidAt":"2025-10-07T15:01:00Z","expiresAt":"2025-11-07T15:00:00Z",
                                 "totalAmount":1000.00,"method":"GATEWAY_BALANCE","referenceCode":"%s",
                                 "fullName":"John Doe","document":"12345678900","phone":"11-99999-8888",
                                 "addressLine1":"Street Name, 123"}""", invoiceId);

                WireMock.stubFor(post(urlMatching("/api/v1/payments"))
                                .willReturn(aResponse().withStatus(201)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody(validBody)));

                PaymentRequest request = PaymentRequest.builder()
                                .invoiceId(invoiceId)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .amount(new BigDecimal("1000.00"))
                                .payer(InvoiceTestDataBuilder.aPayer())
                                .build();

                var payment = service.capture(request);
                assertThat(payment.getGatewayCode()).isEqualTo("pay_abc");
                assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
        }

        private PaymentRequest aGatewayBalanceRequest() {
                return PaymentRequest.builder()
                                .invoiceId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .amount(new BigDecimal("1000.00"))
                                .payer(InvoiceTestDataBuilder.aPayer())
                                .build();
        }
}
