package com.eskcti.algashop.billing.application.invoice.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

@SpringBootTest

@Sql(scripts = "classpath:sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "classpath:sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ActiveProfiles("it")
class InvoiceQueryServiceIT {

  @Autowired
  private InvoiceQueryService invoiceQueryService;

  @Autowired
  private InvoiceRepository invoiceRepository;

  @Test
  void shouldFindByOrderId() {
    Invoice invoice = InvoiceTestDataBuilder.anInvoice()
        .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
        .build();
    invoiceRepository.saveAndFlush(invoice);

    InvoiceOutput invoiceOutput = invoiceQueryService.findByOrderId(invoice.getOrderId());

    assertThat(invoiceOutput.getId()).isEqualTo(invoice.getId());
    assertThat(invoiceOutput.getOrderId()).isEqualTo(invoice.getOrderId());
    assertThat(invoiceOutput.getCustomerId()).isEqualTo(invoice.getCustomerId());
    assertThat(invoiceOutput.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    assertThat(invoiceOutput.getTotalAmount()).isEqualByComparingTo(invoice.getTotalAmount());
    assertThat(invoiceOutput.getPayer().getFullName()).isEqualTo(invoice.getPayer().getFullName());
    assertThat(invoiceOutput.getPayer().getAddress().getCity()).isEqualTo(invoice.getPayer().getAddress().getCity());
    assertThat(invoiceOutput.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    assertThat(invoiceOutput.getPaymentSettings().getCreditCardId()).isNull();
  }

  @Test
  void shouldThrowWhenOrderIdNotFound() {
    assertThatThrownBy(() -> invoiceQueryService.findByOrderId("non-existent-order"))
        .isInstanceOf(InvoiceNotFoundException.class);
  }
}
