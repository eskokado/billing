package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayServiceFastpayImpl implements PaymentGatewayService {

    private final ResilientFastpayPaymentClient resilientClient;
    private final CreditCardRepository creditCardRepository;
    private final AlgaShopPaymentPropreties algaShopPaymentPropreties;

    @Override
    public Payment capture(PaymentRequest request) {
        log.info("Sending payment capture request to Fastpay for invoice {}", request.getInvoiceId());
        FastpayPaymentInput input = convertToInput(request);
        FastpayPaymentModel response = resilientClient.capture(input);
        log.info("Payment capture response received for invoice {}: status={}", request.getInvoiceId(),
                response.getStatus());
        return convertToPayment(response);
    }

    @Override
    public Payment findByCode(String gatewayCode) {
        log.info("Looking up payment on Fastpay by code {}", gatewayCode);
        FastpayPaymentModel response = resilientClient.findById(gatewayCode);
        log.info("Payment lookup succeeded for code {}: status={}", gatewayCode, response.getStatus());
        return convertToPayment(response);
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
            throw new BadGatewayException("Payment gateway returned unexpected response: unknown method: " + response.getMethod(), e);
        }

        FastpayPaymentStatus fastpayPaymentStatus;
        try {
            fastpayPaymentStatus = FastpayPaymentStatus.valueOf(response.getStatus());
        } catch (Exception e) {
            throw new BadGatewayException("Payment gateway returned unexpected response: unknown status: " + response.getStatus(), e);
        }

        builder.method(FastpayEnumConverter.convert(fastpayPaymentMethod));
        builder.status(FastpayEnumConverter.convert(fastpayPaymentStatus));

        return builder.build();
    }
}
