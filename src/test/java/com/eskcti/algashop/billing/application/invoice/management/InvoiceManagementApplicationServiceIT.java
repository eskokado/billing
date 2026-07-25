package com.eskcti.algashop.billing.application.invoice.management;

import static com.eskcti.algashop.billing.application.invoice.management.GenerateInvoiceInputTestDataBuilder.anInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import com.eskcti.algashop.billing.domain.model.DomainException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardTestDataBuilder;
import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder;
import com.eskcti.algashop.billing.domain.model.invoice.InvoicingService;
import com.eskcti.algashop.billing.domain.model.invoice.LineItem;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;

@SpringBootTest
@Transactional
class InvoiceManagementApplicationServiceIT {

  @Autowired
  private InvoiceManagementApplicationService applicationService;

  @Autowired
  private InvoiceRepository invoiceRepository;

  @Autowired
  private CreditCardRepository creditCardRepository;

  @MockitoSpyBean
  private InvoicingService invoicingService;

  @MockitoBean
  private PaymentGatewayService paymentGatewayService;

  @Test
  void shouldGenerateInvoiceWithCreditCardAsPayment() {
    CreditCard creditCard = CreditCardTestDataBuilder.aCreditCard().build();
    creditCardRepository.saveAndFlush(creditCard);

    GenerateInvoiceInput input = anInput().build();
    input.setPaymentSettings(PaymentSettingsInput.builder()
        .creditCardId(creditCard.getId())
        .method(PaymentMethod.CREDIT_CARD)
        .build());

    UUID invoiceId = applicationService.generate(input);

    Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getOrderId()).isEqualTo(input.getOrderId());
    assertThat(invoice.getCustomerId()).isEqualTo(input.getCustomerId());
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("200.00");
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isEqualTo(creditCard.getId());
    assertThat(invoice.getPayer().getFullName()).isEqualTo(input.getPayer().getFullName());
    assertThat(invoice.getPayer().getEmail()).isEqualTo(input.getPayer().getEmail());
    assertThat(invoice.getPayer().getAddress().getCity()).isEqualTo(input.getPayer().getAddress().getCity());

    verify(invoicingService).issue(any(), any(), any(), any());
  }

  @Test
  void shouldGenerateInvoiceWithGatewayBalanceAsPayment() {
    GenerateInvoiceInput input = anInput().build();
    input.setPaymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.GATEWAY_BALANCE)
        .build());

    UUID invoiceId = applicationService.generate(input);

    Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoice.getOrderId()).isEqualTo(input.getOrderId());
    assertThat(invoice.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(invoice.getPaymentSettings().getCreditCardId()).isNull();

    verify(invoicingService).issue(any(), any(), any(), any());
  }

  @Test
  void shouldNumberLineItemsSequentially() {
    GenerateInvoiceInput input = anInput().build();
    input.setPaymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.GATEWAY_BALANCE)
        .build());
    LinkedHashSet<LineItemInput> items = new LinkedHashSet<>();
    items.add(LineItemInput.builder().name("Product 1").amount(new BigDecimal("100.00")).build());
    items.add(LineItemInput.builder().name("Product 2").amount(new BigDecimal("50.00")).build());
    input.setItems(items);

    UUID invoiceId = applicationService.generate(input);

    Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();

    assertThat(invoice.getItems()).hasSize(2);
    assertThat(invoice.getItems()).extracting(LineItem::getNumber).containsExactlyInAnyOrder(1, 2);
    assertThat(invoice.getItems())
        .filteredOn(item -> item.getNumber() == 1)
        .singleElement()
        .satisfies(item -> {
          assertThat(item.getName()).isEqualTo("Product 1");
          assertThat(item.getAmount()).isEqualByComparingTo("100.00");
        });
    assertThat(invoice.getItems())
        .filteredOn(item -> item.getNumber() == 2)
        .singleElement()
        .satisfies(item -> {
          assertThat(item.getName()).isEqualTo("Product 2");
          assertThat(item.getAmount()).isEqualByComparingTo("50.00");
        });
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("150.00");
  }

  @Test
  void shouldThrowWhenCreditCardNotFound() {
    GenerateInvoiceInput input = anInput().build();
    input.setPaymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.CREDIT_CARD)
        .creditCardId(UUID.randomUUID())
        .build());

    assertThatThrownBy(() -> applicationService.generate(input))
        .isInstanceOf(CreditCardNotFoundException.class);
  }

  @Test
  void shouldRejectDuplicateOrder() {
    GenerateInvoiceInput input = anInput().build();
    input.setPaymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.GATEWAY_BALANCE)
        .build());

    applicationService.generate(input);

    assertThatThrownBy(() -> applicationService.generate(input))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Invoice already exists for order");
  }

  @Test
  void shouldProcessInvoicePayment() {
    Invoice invoice = persistUnpaidInvoice();
    Payment payment = aPayment(invoice, PaymentStatus.PAID, "12345");
    when(paymentGatewayService.capture(any(PaymentRequest.class))).thenReturn(payment);

    applicationService.processPayment(invoice.getId());

    Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

    assertThat(paidInvoice.isPaid()).isTrue();
    assertThat(paidInvoice.getPaymentSettings().getGatewayCode()).isEqualTo("12345");

    ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
    verify(paymentGatewayService).capture(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getInvoiceId()).isEqualTo(invoice.getId());
    assertThat(requestCaptor.getValue().getAmount()).isEqualByComparingTo(invoice.getTotalAmount());
    assertThat(requestCaptor.getValue().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    verify(invoicingService).assignPayment(any(Invoice.class), any(Payment.class));
  }

  @Test
  void shouldThrowWhenInvoiceNotFoundOnProcessPayment() {
    assertThatThrownBy(() -> applicationService.processPayment(UUID.randomUUID()))
        .isInstanceOf(InvoiceNotFoundException.class);
  }

  @Test
  void shouldCancelInvoiceWhenPaymentCaptureFails() {
    Invoice invoice = persistUnpaidInvoice();
    when(paymentGatewayService.capture(any(PaymentRequest.class)))
        .thenThrow(new RuntimeException("gateway unavailable"));

    applicationService.processPayment(invoice.getId());

    Invoice canceledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

    assertThat(canceledInvoice.isCanceled()).isTrue();
    assertThat(canceledInvoice.getCancelReason()).isEqualTo("Payment capture failed");
    verify(invoicingService, never()).assignPayment(any(Invoice.class), any(Payment.class));
  }

  @Test
  void shouldCancelInvoiceWhenPaymentFails() {
    Invoice invoice = persistUnpaidInvoice();
    Payment payment = aPayment(invoice, PaymentStatus.FAILED, "12345");
    when(paymentGatewayService.capture(any(PaymentRequest.class))).thenReturn(payment);

    applicationService.processPayment(invoice.getId());

    Invoice canceledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

    assertThat(canceledInvoice.isCanceled()).isTrue();
    assertThat(canceledInvoice.getCancelReason()).isEqualTo("Payment failed");
    assertThat(canceledInvoice.getPaymentSettings().getGatewayCode()).isEqualTo("12345");
  }

  @Test
  void shouldCancelInvoiceWhenPaymentIsRefunded() {
    Invoice invoice = persistUnpaidInvoice();
    Payment payment = aPayment(invoice, PaymentStatus.REFUNDED, "12345");
    when(paymentGatewayService.capture(any(PaymentRequest.class))).thenReturn(payment);

    applicationService.processPayment(invoice.getId());

    Invoice canceledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

    assertThat(canceledInvoice.isCanceled()).isTrue();
    assertThat(canceledInvoice.getCancelReason()).isEqualTo("Payment refunded");
  }

  private Invoice persistUnpaidInvoice() {
    Invoice invoice = InvoiceTestDataBuilder.anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();
    return invoiceRepository.saveAndFlush(invoice);
  }

  private Payment aPayment(Invoice invoice, PaymentStatus status, String gatewayCode) {
    return Payment.builder()
        .gatewayCode(gatewayCode)
        .invoiceId(invoice.getId())
        .method(invoice.getPaymentSettings().getMethod())
        .status(status)
        .build();
  }
}
