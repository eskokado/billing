package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import java.net.SocketTimeoutException;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.invoice.Address;
import com.eskcti.algashop.billing.domain.model.invoice.Payer;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.infrastructure.payment.AlgaShopPaymentPropreties;
import com.eskcti.algashop.billing.presentation.BadGatewayException;
import com.eskcti.algashop.billing.presentation.GatewayTimeoutException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayServiceFastpayImpl implements PaymentGatewayService {

    private final FastpayPaymentAPIClient fastpayPaymentAPIClient;
    private final CreditCardRepository creditCardRepository;

    private final AlgaShopPaymentPropreties algaShopPaymentPropreties;

    @Override
    public Payment capture(PaymentRequest request) {
        FastpayPaymentInput input = convertToInput(request);
        log.info("Sending payment capture request to Fastpay for invoice {}", request.getInvoiceId());
        try {
            FastpayPaymentModel response = fastpayPaymentAPIClient.capture(input);
            log.info("Payment capture response received for invoice {}: status={}", request.getInvoiceId(),
                    response.getStatus());
            return convertToPayment(response);
        } catch (ResourceAccessException | QueryTimeoutException ex) {
            if (ex.getCause() instanceof SocketTimeoutException
                    || ex.getMessage() != null && (ex.getMessage().toLowerCase().contains("timeout")
                            || ex.getMessage().toLowerCase().contains("timed out")
                            || ex.getMessage().toLowerCase().contains("time-out"))) {
                throw new GatewayTimeoutException("Payment gateway timed out while capturing payment", ex);
            }
            throw new BadGatewayException("Payment gateway is unavailable while capturing payment", ex);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw ex;
            }
            throw new BadGatewayException(
                    "Payment gateway responded with client error while capturing payment: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new GatewayTimeoutException(
                    "Payment gateway responded with server error while capturing payment: " + ex.getStatusCode(), ex);
        } catch (ErrorResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status != null && status.is5xxServerError()) {
                throw new GatewayTimeoutException(
                        "Payment gateway responded with server error while capturing payment: " + status, ex);
            }
            throw new BadGatewayException(
                    "Payment gateway responded with client error while capturing payment: " + status, ex);
        } catch (IllegalArgumentException ex) {
            throw new BadGatewayException("Payment gateway returned unexpected response: " + ex.getMessage(), ex);
        } catch (CreditCardNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadGatewayException("Unexpected error while communicating with payment gateway", ex);
        }
    }

    @Override
    public Payment findByCode(String gatewayCode) {
        log.info("Looking up payment on Fastpay by code {}", gatewayCode);
        try {
            FastpayPaymentModel response = fastpayPaymentAPIClient.findById(gatewayCode);
            log.info("Payment lookup succeeded for code {}: status={}", gatewayCode, response.getStatus());
            return convertToPayment(response);
        } catch (ResourceAccessException | QueryTimeoutException ex) {
            if (ex.getCause() instanceof SocketTimeoutException
                    || ex.getMessage() != null && (ex.getMessage().toLowerCase().contains("timeout")
                            || ex.getMessage().toLowerCase().contains("timed out")
                            || ex.getMessage().toLowerCase().contains("time-out"))) {
                throw new GatewayTimeoutException("Payment gateway timed out while looking up payment", ex);
            }
            throw new BadGatewayException("Payment gateway is unavailable while looking up payment", ex);
        } catch (HttpClientErrorException.NotFound ex) {
            throw ex;
        } catch (HttpClientErrorException ex) {
            throw new BadGatewayException(
                    "Payment gateway responded with client error while looking up payment: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new GatewayTimeoutException(
                    "Payment gateway responded with server error while looking up payment: " + ex.getStatusCode(), ex);
        } catch (ErrorResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status != null && status.is5xxServerError()) {
                throw new GatewayTimeoutException(
                        "Payment gateway responded with server error while looking up payment: " + status, ex);
            }
            throw new BadGatewayException(
                    "Payment gateway responded with client error while looking up payment: " + status, ex);
        } catch (IllegalArgumentException ex) {
            throw new BadGatewayException("Payment gateway returned unexpected response: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new BadGatewayException("Unexpected error while communicating with payment gateway", ex);
        }
    }

    private FastpayPaymentInput convertToInput(PaymentRequest request) {
        Payer payer = request.getPayer();
        Address address = payer.getAddress();

        var builder = FastpayPaymentInput.builder()
                .totalAmount(request.getAmount())
                .referenceCode(request.getInvoiceId().toString())
                .fullName(payer.getFullName())
                .document(payer.getDocument())
                .phone(payer.getPhone())
                .zipCode(address.getZipCode())
                .addressLine1(address.getStreet() + ", " + address.getNumber())
                .addressLine2(address.getComplement())
                .replyToUrl(algaShopPaymentPropreties.getFastpay().getWebhookUrl());

        if (request.getMethod() == PaymentMethod.CREDIT_CARD) {
            builder.method(FastpayPaymentMethod.CREDIT.name());
            CreditCard creditCard = creditCardRepository.findById(request.getCreditCardId())
                    .orElseThrow(() -> new CreditCardNotFoundException());
            builder.creditCardId(creditCard.getGatewayCode());
        } else {
            builder.method(FastpayPaymentMethod.GATEWAY_BALANCE.name());
        }

        return builder.build();
    }

    private Payment convertToPayment(FastpayPaymentModel response) {
        var builder = Payment.builder()
                .gatewayCode(response.getId())
                .invoiceId(UUID.fromString(response.getReferenceCode()));

        FastpayPaymentMethod fastpayPaymentMethod;

        try {
            fastpayPaymentMethod = FastpayPaymentMethod.valueOf(response.getMethod());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown payment method: " + response.getMethod());
        }

        FastpayPaymentStatus fastpayPaymentStatus;
        try {
            fastpayPaymentStatus = FastpayPaymentStatus.valueOf(response.getStatus());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown payment status: " + response.getStatus());
        }

        builder.method(FastpayEnumConverter.convert(fastpayPaymentMethod));
        builder.status(FastpayEnumConverter.convert(fastpayPaymentStatus));

        return builder.build();
    }
}
