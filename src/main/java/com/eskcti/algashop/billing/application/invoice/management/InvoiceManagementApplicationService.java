package com.eskcti.algashop.billing.application.invoice.management;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.invoice.Address;
import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.eskcti.algashop.billing.domain.model.invoice.InvoicingService;
import com.eskcti.algashop.billing.domain.model.invoice.LineItem;
import com.eskcti.algashop.billing.domain.model.invoice.Payer;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceManagementApplicationService {

  private final PaymentGatewayService paymentGatewayService;
  private final InvoicingService invoicingService;
  private final InvoiceRepository invoiceRepository;
  private final CreditCardRepository creditCardRepository;

  @Transactional
  public UUID generate(GenerateInvoiceInput input) {
    PaymentSettingsInput paymentSettings = input.getPaymentSettings();
    verifyCreditCardId(paymentSettings.getCreditCardId());

    Payer payer = convertToPayer(input.getPayer());
    Set<LineItem> items = convertToLineItems(input.getItems());

    Invoice invoice = invoicingService.issue(input.getOrderId(), input.getCustomerId(), payer, items);
    invoice.changePaymentSettings(paymentSettings.getMethod(), paymentSettings.getCreditCardId());

    invoiceRepository.saveAndFlush(invoice);

    return invoice.getId();
  }

  @Transactional
  public void processPayment(UUID invoiceId) {
    Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException());
    PaymentRequest paymentRequest = toPaymentRequest(invoice);

    Payment payment;
    try {
      payment = paymentGatewayService.capture(paymentRequest);
    } catch (Exception e) {
      String errorMessage = "Payment capture failed";
      log.error(errorMessage, e);
      invoice.cancel(errorMessage);
      invoiceRepository.saveAndFlush(invoice);
      return;
    }

    invoicingService.assignPayment(invoice, payment);
    invoiceRepository.saveAndFlush(invoice);
  }

  private PaymentRequest toPaymentRequest(Invoice invoice) {
    return PaymentRequest.builder()
        .amount(invoice.getTotalAmount())
        .method(invoice.getPaymentSettings().getMethod())
        .creditCardId(invoice.getPaymentSettings().getCreditCardId())
        .payer(invoice.getPayer())
        .invoiceId(invoice.getId())
        .build();
  }

  private Set<LineItem> convertToLineItems(Set<LineItemInput> itemsInput) {
    Set<LineItem> lineItems = new LinkedHashSet<>();
    int itemNumber = 1;
    for (LineItemInput itemInput : itemsInput) {
      lineItems.add(LineItem.builder()
          .number(itemNumber)
          .name(itemInput.getName())
          .amount(itemInput.getAmount())
          .build());
      itemNumber++;
    }
    return lineItems;
  }

  private Payer convertToPayer(PayerData payerData) {
    AddressData addressData = payerData.getAddress();

    return Payer.builder()
        .fullName(payerData.getFullName())
        .email(payerData.getEmail())
        .document(payerData.getDocument())
        .phone(payerData.getPhone())
        .address(Address.builder()
            .city(addressData.getCity())
            .state(addressData.getState())
            .neighborhood(addressData.getNeighborhood())
            .complement(addressData.getComplement())
            .zipCode(addressData.getZipCode())
            .street(addressData.getStreet())
            .number(addressData.getNumber())
            .build())
        .build();
  }

  private void verifyCreditCardId(UUID creditCardId) {
    if (creditCardId != null && !creditCardRepository.existsById(creditCardId)) {
      throw new CreditCardNotFoundException();
    }
  }
}