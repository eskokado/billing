package com.eskcti.algashop.billing.application.invoice.management;

import static com.eskcti.algashop.billing.application.invoice.management.GenerateInvoiceInputTestDataBuilder.anInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import com.eskcti.algashop.billing.domain.model.DomainException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCard;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardTestDataBuilder;
import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.eskcti.algashop.billing.domain.model.invoice.InvoicingService;
import com.eskcti.algashop.billing.domain.model.invoice.LineItem;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

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
    input.setItems(Set.of(
        LineItemInput.builder().name("Product 1").amount(new BigDecimal("100.00")).build(),
        LineItemInput.builder().name("Product 2").amount(new BigDecimal("50.00")).build()));

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
}
