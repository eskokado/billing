package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import java.net.SocketTimeoutException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.eskcti.algashop.billing.presentation.BadGatewayException;
import com.eskcti.algashop.billing.presentation.GatewayTimeoutException;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
@Slf4j
public class ResilientFastpayPaymentClient {

    private final FastpayPaymentAPIClient fastpayPaymentAPIClient;

    @Bulkhead(name = "fastpayPayment")
    @CircuitBreaker(name = "fastpayPayment", fallbackMethod = "captureFallback")
    @Retry(name = "fastpayPayment", fallbackMethod = "captureFallback")
    public FastpayPaymentModel capture(FastpayPaymentInput input) {
        log.info("Calling Fastpay capture via resilient client");
        try {
            FastpayPaymentModel response = fastpayPaymentAPIClient.capture(input);
            log.info("Fastpay capture succeeded: status={}", response.getStatus());
            return response;
        } catch (ResourceAccessException | QueryTimeoutException ex) {
            throw translateResourceAccessException(ex, "capturing");
        } catch (HttpClientErrorException.NotFound ex) {
            throw ex;
        } catch (HttpClientErrorException ex) {
            throw new BadGatewayException(
                    "Payment gateway responded with client error while capturing: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new GatewayTimeoutException(
                    "Payment gateway responded with server error while capturing: " + ex.getStatusCode(), ex);
        } catch (ErrorResponseException ex) {
            throw translateErrorResponseException(ex, "capturing");
        } catch (Exception ex) {
            throw new BadGatewayException("Unexpected error while capturing payment", ex);
        }
    }

    @Bulkhead(name = "fastpayPayment")
    @CircuitBreaker(name = "fastpayPayment", fallbackMethod = "findByIdFallback")
    @Retry(name = "fastpayPayment", fallbackMethod = "findByIdFallback")
    public FastpayPaymentModel findById(String paymentId) {
        log.info("Calling Fastpay findById via resilient client for id {}", paymentId);
        try {
            FastpayPaymentModel response = fastpayPaymentAPIClient.findById(paymentId);
            log.info("Fastpay findById succeeded for id {}: status={}", paymentId, response.getStatus());
            return response;
        } catch (ResourceAccessException | QueryTimeoutException ex) {
            throw translateResourceAccessException(ex, "looking up");
        } catch (HttpClientErrorException.NotFound ex) {
            throw ex;
        } catch (HttpClientErrorException ex) {
            throw new BadGatewayException(
                    "Payment gateway responded with client error while looking up: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new GatewayTimeoutException(
                    "Payment gateway responded with server error while looking up: " + ex.getStatusCode(), ex);
        } catch (ErrorResponseException ex) {
            throw translateErrorResponseException(ex, "looking up");
        } catch (Exception ex) {
            throw new BadGatewayException("Unexpected error while looking up payment", ex);
        }
    }

    private FastpayPaymentModel captureFallback(Throwable ex) {
        log.warn("Fastpay capture failed, circuit breaker/retry exhausted", ex);
        if (ex instanceof GatewayTimeoutException) {
            throw (GatewayTimeoutException) ex;
        }
        if (ex instanceof BadGatewayException) {
            throw (BadGatewayException) ex;
        }
        throw new BadGatewayException("Payment gateway is unavailable while capturing payment", ex);
    }

    private FastpayPaymentModel findByIdFallback(Throwable ex) {
        log.warn("Fastpay findById failed, circuit breaker/retry exhausted", ex);
        if (ex instanceof GatewayTimeoutException) {
            throw (GatewayTimeoutException) ex;
        }
        if (ex instanceof BadGatewayException) {
            throw (BadGatewayException) ex;
        }
        throw new BadGatewayException("Payment gateway is unavailable while looking up payment", ex);
    }

    private RuntimeException translateResourceAccessException(Exception ex, String operation) {
        if (ex.getCause() instanceof SocketTimeoutException
                || ex.getMessage() != null && (ex.getMessage().toLowerCase().contains("timeout")
                        || ex.getMessage().toLowerCase().contains("timed out")
                        || ex.getMessage().toLowerCase().contains("time-out"))) {
            return new GatewayTimeoutException(
                    "Payment gateway timed out while " + operation + " payment", ex);
        }
        return new BadGatewayException(
                "Payment gateway is unavailable while " + operation + " payment", ex);
    }

    private RuntimeException translateErrorResponseException(ErrorResponseException ex, String operation) {
        int code = ex.getStatusCode().value();
        if (code >= 500) {
            return new GatewayTimeoutException(
                    "Payment gateway responded with server error while " + operation + ": " + ex.getStatusCode(), ex);
        }
        return new BadGatewayException(
                "Payment gateway responded with client error while " + operation + ": " + ex.getStatusCode(), ex);
    }
}
