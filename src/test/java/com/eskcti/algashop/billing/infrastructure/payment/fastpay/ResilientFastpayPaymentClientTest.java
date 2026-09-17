package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.eskcti.algashop.billing.presentation.BadGatewayException;
import com.eskcti.algashop.billing.presentation.GatewayTimeoutException;

@ExtendWith(MockitoExtension.class)
class ResilientFastpayPaymentClientTest {

        @Mock
        private FastpayPaymentAPIClient fastpayPaymentAPIClient;

        @InjectMocks
        private ResilientFastpayPaymentClient client;

        private static final String DEFAULT_PAYMENT_ID = "pay-xyz-789";

        private static FastpayPaymentInput aCaptureInput() {
                return FastpayPaymentInput.builder()
                                .totalAmount(new java.math.BigDecimal("100.00"))
                                .referenceCode("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                                .fullName("John Doe")
                                .document("12345")
                                .phone("11999998888")
                                .zipCode("12345")
                                .addressLine1("Street, 123")
                                .method("GATEWAY_BALANCE")
                                .build();
        }

        private static FastpayPaymentModel aResponse(String id, String method, String status) {
                return FastpayPaymentModel.builder()
                                .id(id)
                                .referenceCode("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                                .method(method)
                                .status(status)
                                .build();
        }

        @Test
        void shouldCaptureSuccessfully() {
                FastpayPaymentModel response = aResponse(DEFAULT_PAYMENT_ID, "GATEWAY_BALANCE", "PAID");
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class))).thenReturn(response);

                FastpayPaymentModel result = client.capture(aCaptureInput());

                assertThat(result.getId()).isEqualTo(DEFAULT_PAYMENT_ID);
                assertThat(result.getStatus()).isEqualTo("PAID");
        }

        @Test
        void shouldFindByIdSuccessfully() {
                FastpayPaymentModel response = aResponse(DEFAULT_PAYMENT_ID, "GATEWAY_BALANCE", "PAID");
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID)).thenReturn(response);

                FastpayPaymentModel result = client.findById(DEFAULT_PAYMENT_ID);

                assertThat(result.getId()).isEqualTo(DEFAULT_PAYMENT_ID);
        }

        @Test
        void shouldThrowGatewayTimeoutOnSocketTimeoutDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("I/O error",
                                                new SocketTimeoutException("connect timed out")));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnQueryTimeoutDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new QueryTimeoutException("query timeout: timed out",
                                                new RuntimeException()));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessExceptionWithoutTimeout() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("Connection refused",
                                                new ConnectException("Connection refused")));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnHttpClientErrorDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldRethrowNotFoundDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found",
                                                null, null, null));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        void shouldThrowGatewayTimeoutOnHttpServerErrorDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Internal Server Error"));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnErrorResponseException4xxDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnErrorResponseException5xxDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(HttpStatus.BAD_GATEWAY));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnUnexpectedExceptionDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new RuntimeException("kaboom"));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("Unexpected error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnSocketTimeoutDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("I/O error",
                                                new SocketTimeoutException("Read timed out")));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessExceptionWithoutTimeoutDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("Connection refused",
                                                new ConnectException("Connection refused")));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnHttpClientErrorDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldRethrowNotFoundDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found",
                                                null, null, null));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        void shouldThrowGatewayTimeoutOnHttpServerErrorDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Internal Server Error"));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnErrorResponseException4xxDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(HttpStatus.BAD_REQUEST));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnErrorResponseException5xxDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(HttpStatus.BAD_GATEWAY));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnUnexpectedExceptionDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new RuntimeException("kaboom"));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("Unexpected error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnErrorResponseExceptionWithUnknownHighStatusCodeDuringCapture() {
                HttpStatusCode weird = HttpStatusCode.valueOf(999);
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(weird));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowBadGatewayOnErrorResponseExceptionWithUnknownLowStatusCodeDuringCapture() {
                HttpStatusCode weird = HttpStatusCode.valueOf(499);
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ErrorResponseException(weird));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowBadGatewayOnErrorResponseExceptionWithUnknownStatusCodeDuringFindById() {
                HttpStatusCode weird = HttpStatusCode.valueOf(499);
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(weird));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("client error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnErrorResponseExceptionWithUnknownHighStatusCodeDuringFindById() {
                HttpStatusCode weird = HttpStatusCode.valueOf(999);
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ErrorResponseException(weird));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("server error");
        }

        @Test
        void shouldThrowGatewayTimeoutOnQueryTimeoutDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new QueryTimeoutException("query timeout: timed out",
                                                new RuntimeException()));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnResourceAccessWithTimeoutWordDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("Read timed out while writing",
                                                new ConnectException()));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnResourceAccessWithTimeOutHyphenDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException("restclient time-out expired",
                                                new ConnectException()));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessWithNullMessageDuringCapture() {
                when(fastpayPaymentAPIClient.capture(any(FastpayPaymentInput.class)))
                                .thenThrow(new ResourceAccessException(null,
                                                new ConnectException("Connection refused")));

                assertThatThrownBy(() -> client.capture(aCaptureInput()))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowBadGatewayOnResourceAccessWithNullMessageDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException(null,
                                                new ConnectException("Refused")));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldThrowGatewayTimeoutOnResourceAccessWithTimeoutWordDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("Read timed out while reading",
                                                new ConnectException()));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldThrowGatewayTimeoutOnResourceAccessWithTimeOutHyphenDuringFindById() {
                when(fastpayPaymentAPIClient.findById(DEFAULT_PAYMENT_ID))
                                .thenThrow(new ResourceAccessException("restclient time-out expired",
                                                new ConnectException()));

                assertThatThrownBy(() -> client.findById(DEFAULT_PAYMENT_ID))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timed out");
        }

        @Test
        void shouldCaptureFallbackRethrowGatewayTimeoutException() throws Exception {
                Method method = ResilientFastpayPaymentClient.class
                                .getDeclaredMethod("captureFallback", Throwable.class);
                method.setAccessible(true);

                GatewayTimeoutException ex = new GatewayTimeoutException("timeout",
                                new RuntimeException("root"));

                assertThatThrownBy(() -> invokeUnchecked(method, ex))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timeout");
        }

        @Test
        void shouldCaptureFallbackRethrowBadGatewayException() throws Exception {
                Method method = ResilientFastpayPaymentClient.class
                                .getDeclaredMethod("captureFallback", Throwable.class);
                method.setAccessible(true);

                BadGatewayException ex = new BadGatewayException("gateway down", new RuntimeException("root"));

                assertThatThrownBy(() -> invokeUnchecked(method, ex))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("gateway down");
        }

        @Test
        void shouldCaptureFallbackWrapOtherThrowableInBadGatewayException() throws Exception {
                Method method = ResilientFastpayPaymentClient.class
                                .getDeclaredMethod("captureFallback", Throwable.class);
                method.setAccessible(true);

                RuntimeException ex = new RuntimeException("unexpected");

                assertThatThrownBy(() -> invokeUnchecked(method, ex))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        @Test
        void shouldFindByIdFallbackRethrowGatewayTimeoutException() throws Exception {
                Method method = ResilientFastpayPaymentClient.class
                                .getDeclaredMethod("findByIdFallback", Throwable.class);
                method.setAccessible(true);

                GatewayTimeoutException ex = new GatewayTimeoutException("timeout",
                                new RuntimeException("root"));

                assertThatThrownBy(() -> invokeUnchecked(method, ex))
                                .isInstanceOf(GatewayTimeoutException.class)
                                .hasMessageContaining("timeout");
        }

        @Test
        void shouldFindByIdFallbackRethrowBadGatewayException() throws Exception {
                Method method = ResilientFastpayPaymentClient.class
                                .getDeclaredMethod("findByIdFallback", Throwable.class);
                method.setAccessible(true);

                BadGatewayException ex = new BadGatewayException("gateway down", new RuntimeException("root"));

                assertThatThrownBy(() -> invokeUnchecked(method, ex))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("gateway down");
        }

        @Test
        void shouldFindByIdFallbackWrapOtherThrowableInBadGatewayException() throws Exception {
                Method method = ResilientFastpayPaymentClient.class
                                .getDeclaredMethod("findByIdFallback", Throwable.class);
                method.setAccessible(true);

                RuntimeException ex = new RuntimeException("unexpected");

                assertThatThrownBy(() -> invokeUnchecked(method, ex))
                                .isInstanceOf(BadGatewayException.class)
                                .hasMessageContaining("unavailable");
        }

        private void invokeUnchecked(Method method, Object... args) throws Throwable {
                try {
                        method.invoke(client, args);
                } catch (InvocationTargetException e) {
                        throw e.getCause();
                }
        }
}
