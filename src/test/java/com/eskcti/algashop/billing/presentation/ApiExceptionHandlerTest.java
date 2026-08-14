package com.eskcti.algashop.billing.presentation;

import com.eskcti.algashop.billing.domain.model.DomainEntityNotFoundException;
import com.eskcti.algashop.billing.domain.model.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler(messageSource);
    }

    @Test
    void shouldReturn404WhenHandlingDomainEntityNotFoundException() {
        DomainEntityNotFoundException ex = new DomainEntityNotFoundException("Invoice not found");

        ProblemDetail pd = handler.handleDomainEntityNotFoundException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Not found");
        assertThat(pd.getDetail()).isEqualTo("Invoice not found");
        assertThat(pd.getType()).isEqualTo(URI.create("/errors/not-found"));
    }

    @Test
    void shouldReturn422WhenHandlingDomainException() {
        DomainException ex = new DomainException("Some business rule");

        ProblemDetail pd = handler.handleUnprocessableEntityException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(pd.getTitle()).isEqualTo("Unprocessable Entity");
        assertThat(pd.getDetail()).isEqualTo("Some business rule");
        assertThat(pd.getType()).isEqualTo(URI.create("/errors/unprocessable-entity"));
    }

    @Test
    void shouldReturn504WhenHandlingGatewayTimeout() {
        RuntimeException cause = new RuntimeException("root");
        GatewayTimeoutException ex = new GatewayTimeoutException("Payment gateway timed out", cause);

        ProblemDetail pd = handler.handleGatewayTimeoutException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(pd.getTitle()).isEqualTo("Gateway Timeout");
        assertThat(pd.getDetail()).isEqualTo("Payment gateway timed out");
        assertThat(pd.getType()).isEqualTo(URI.create("/errors/gateway-timeout"));
    }

    @Test
    void shouldReturn502WhenHandlingBadGateway() {
        RuntimeException cause = new RuntimeException("broken");
        BadGatewayException ex = new BadGatewayException("Gateway is broken", cause);

        ProblemDetail pd = handler.handleBadGatewayException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(pd.getTitle()).isEqualTo("Bad Gateway");
        assertThat(pd.getDetail()).isEqualTo("Gateway is broken");
        assertThat(pd.getType()).isEqualTo(URI.create("/errors/bad-gateway"));
    }

    @Test
    void shouldReturn500WhenHandlingGenericException() {
        RuntimeException ex = new RuntimeException("boom");

        ProblemDetail pd = handler.handleException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
        assertThat(pd.getDetail()).isEqualTo("An unexpected internal error occurred.");
        assertThat(pd.getType()).isEqualTo(URI.create("/errors/internal"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturn400WithFieldErrorsWhenHandlingMethodArgumentNotValid() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("generateInvoiceInput", "orderId", "orderId is required");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(messageSource.getMessage(eq(fieldError), any(Locale.class))).thenReturn("orderId is required");

        TestableApiExceptionHandler testable = new TestableApiExceptionHandler(messageSource);
        HttpHeaders headers = new HttpHeaders();
        HttpStatusCode status = HttpStatus.BAD_REQUEST;
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> response = testable.handleMethodArgumentNotValid(ex, headers, status, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail pd = (ProblemDetail) response.getBody();
        assertThat(pd.getTitle()).isEqualTo("Invalid fields");
        assertThat(pd.getType()).isEqualTo(URI.create("/errors/invalid-fields"));
        Map<String, String> fields = (Map<String, String>) pd.getProperties().get("fields");
        assertThat(fields).containsEntry("orderId", "orderId is required");
    }

    private static class TestableApiExceptionHandler extends ApiExceptionHandler {
        public TestableApiExceptionHandler(MessageSource messageSource) {
            super(messageSource);
        }

        @Override
        public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
            return super.handleMethodArgumentNotValid(ex, headers, status, request);
        }
    }
}
