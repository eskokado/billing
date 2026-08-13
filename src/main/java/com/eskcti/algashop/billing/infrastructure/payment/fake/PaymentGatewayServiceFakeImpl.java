package com.eskcti.algashop.billing.infrastructure.payment.fake;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;

@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FAKE")
public class PaymentGatewayServiceFakeImpl implements PaymentGatewayService {

  @Override
  public Payment capture(PaymentRequest request) {
    return Payment.builder()
        .invoiceId(request.getInvoiceId())
        .status(PaymentStatus.PAID)
        .method(request.getMethod())
        .gatewayCode(UUID.randomUUID().toString())
        .build();
  }

  @Override
  public Payment findByCode(String gatewayCode) {
    return Payment.builder()
        .invoiceId(UUID.randomUUID())
        .status(PaymentStatus.PAID)
        .method(PaymentMethod.GATEWAY_BALANCE)
        .gatewayCode(UUID.randomUUID().toString())
        .build();
  }
}